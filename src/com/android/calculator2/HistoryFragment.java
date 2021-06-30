/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.calculator2;

import android.animation.Animator;
import android.app.Fragment;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import static androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_DRAGGING;

public class HistoryFragment extends Fragment implements DragLayout.DragCallback {

    public static final String TAG = "HistoryFragment";
    public static final String CLEAR_DIALOG_TAG = "clear";

    private final DragController mDragController = new DragController();

    private RecyclerView mRecyclerView;
    private HistoryAdapter mAdapter;
    private DragLayout mDragLayout;
    private Toolbar mToolbar;

    private Evaluator mEvaluator;

    private ArrayList<HistoryItem> mDataSet = new ArrayList<>();

    private boolean mIsDisplayEmpty;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAdapter = new HistoryAdapter(mDataSet);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(
                R.layout.fragment_history, container, false /* attachToRoot */);

        mDragLayout = (DragLayout) container.getRootView().findViewById(R.id.drag_layout);
        mDragLayout.addDragCallback(this);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.history_recycler_view);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == SCROLL_STATE_DRAGGING) {
                    stopActionModeOrContextMenu();
                }
                super.onScrollStateChanged(recyclerView, newState);
            }
        });

        // The size of the RecyclerView is not affected by the adapter's contents.
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setAdapter(mAdapter);

        mToolbar = (Toolbar) view.findViewById(R.id.history_toolbar);
        mToolbar.inflateMenu(R.menu.fragment_history);
        mToolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_clear_history) {
                final Calculator calculator = (Calculator) getActivity();
                AlertDialogFragment.showMessageDialog(calculator, "" /* title */,
                        getString(R.string.dialog_clear),
                        getString(R.string.menu_clear_history),
                        CLEAR_DIALOG_TAG);
                return true;
            }
            return onOptionsItemSelected(item);
        });
        mToolbar.setNavigationOnClickListener(v -> getActivity().onBackPressed());
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final Calculator activity = (Calculator) getActivity();
        mEvaluator = Evaluator.getInstance(activity);
        mAdapter.setEvaluator(mEvaluator);

        final boolean isResultLayout = activity.isResultLayout();
        final boolean isOneLine = activity.isOneLine();

        // Snapshot display state here. For the rest of the lifecycle of this current
        // HistoryFragment, this is what we will consider the display state.
        // In rare cases, the display state can change after our adapter is initialized.
        final CalculatorExpr mainExpr = mEvaluator.getExpr(Evaluator.MAIN_INDEX);
        mIsDisplayEmpty = mainExpr == null || mainExpr.isEmpty();

        initializeController(isResultLayout, isOneLine, mIsDisplayEmpty);

        final long maxIndex = mEvaluator.getMaxIndex();

        final ArrayList<HistoryItem> newDataSet = new ArrayList<>();

        if (!mIsDisplayEmpty && !isResultLayout) {
            // Add the current expression as the first element in the list (the layout is
            // reversed and we want the current expression to be the last one in the
            // RecyclerView).
            // If we are in the result state, the result will animate to the last history
            // element in the list and there will be no "Current Expression."
            mEvaluator.copyMainToHistory();
            newDataSet.add(new HistoryItem(Evaluator.HISTORY_MAIN_INDEX,
                    System.currentTimeMillis(), mEvaluator.getExprAsSpannable(0)));
        }
        for (long i = 0; i < maxIndex; ++i) {
            newDataSet.add(null);
        }
        final boolean isEmpty = newDataSet.isEmpty();
        mRecyclerView.setBackgroundColor(ContextCompat.getColor(activity,
                isEmpty ? R.color.empty_history_color : R.color.display_background_color));
        if (isEmpty) {
            newDataSet.add(new HistoryItem());
        }
        mDataSet = newDataSet;
        mAdapter.setDataSet(mDataSet);
        mAdapter.setIsResultLayout(isResultLayout);
        mAdapter.setIsOneLine(activity.isOneLine());
        mAdapter.setIsDisplayEmpty(mIsDisplayEmpty);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onStart() {
        super.onStart();

        final Calculator activity = (Calculator) getActivity();
        mDragController.initializeAnimation(activity.isResultLayout(), activity.isOneLine(),
                mIsDisplayEmpty);
        initMenuIcon();
    }

    @Override
    public Animator onCreateAnimator(int transit, boolean enter, int nextAnim) {
        return mDragLayout.createAnimator(enter);
    }

    @Override
    public void onResume() {
        super.onResume();
        initMenuIcon();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mDragLayout != null) {
            mDragLayout.removeDragCallback(this);
        }

        if (mEvaluator != null) {
            // Note that the view is destroyed when the fragment backstack is popped, so
            // these are essentially called when the DragLayout is closed.
            mEvaluator.cancelNonMain();
        }
    }

    private void initializeController(boolean isResult, boolean isOneLine, boolean isDisplayEmpty) {
        mDragController.setDisplayFormula(
                (CalculatorFormula) getActivity().findViewById(R.id.formula));
        mDragController.setDisplayResult(
                (CalculatorResult) getActivity().findViewById(R.id.result));
        mDragController.setToolbar(getActivity().findViewById(R.id.toolbar));
        mDragController.setEvaluator(mEvaluator);
        mDragController.initializeController(isResult, isOneLine, isDisplayEmpty);
    }

    public boolean stopActionModeOrContextMenu() {
        if (mRecyclerView == null) {
            return false;
        }
        for (int i = 0; i < mRecyclerView.getChildCount(); i++) {
            final View view = mRecyclerView.getChildAt(i);
            final HistoryAdapter.ViewHolder viewHolder =
                    (HistoryAdapter.ViewHolder) mRecyclerView.getChildViewHolder(view);
            if (viewHolder != null && viewHolder.getResult() != null
                    && viewHolder.getResult().stopActionModeOrContextMenu()) {
                return true;
            }
        }
        return false;
    }

    private void initMenuIcon() {
        int size = mDataSet.size();
        int id = R.drawable.ic_clear;
        Resources resources = getResources();
        MenuItem menuItem = mToolbar.getMenu().getItem(0);
        if (size >= 2) {
            menuItem.setEnabled(true);
        } else {
            HistoryItem item = mDataSet.get(0);
            if (item == null) {
                menuItem.setEnabled(true);
                menuItem.setIcon(resources.getDrawable(id));
                return;
            }
            boolean isEnable = !item.isEmptyView();
            menuItem.setEnabled(isEnable);
            if (!isEnable) id = R.drawable.ic_clear_disabled;
        }
        menuItem.setIcon(resources.getDrawable(id));
    }

    /* Begin override DragCallback methods. */

    @Override
    public void onStartDraggingOpen() {
        // no-op
    }

    @Override
    public void onInstanceStateRestored(boolean isOpen) {
        if (isOpen) {
            mRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void whileDragging(float yFraction) {
        if (isVisible() || isRemoving()) {
            mDragController.animateViews(yFraction, mRecyclerView);
        }
    }

    @Override
    public boolean shouldCaptureView(View view, int x, int y) {
        return !mRecyclerView.canScrollVertically(1 /* scrolling down */);
    }

    @Override
    public int getDisplayHeight() {
        return 0;
    }

    /* End override DragCallback methods. */
}
