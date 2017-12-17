package com.ternaryop.photoshelf.dialogs.mru;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;

import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.view.SwipeToDeleteCallback;

/**
 * Created by dave on 17/05/15.
 * Allow to select items from MRU
 */
public class MRUDialog extends DialogFragment {
    public static final String ARG_MRU_LIST = "mruList";

    private MRUAdapter adapter;
    private OnMRUListener onMRUListener;

    public static MRUDialog newInstance(List<String> list) {
        Bundle args = new Bundle();
        args.putStringArrayList(MRUDialog.ARG_MRU_LIST, new ArrayList<>(list));

        MRUDialog fragment = new MRUDialog();
        fragment.setArguments(args);
        return fragment;
    }

    public MRUDialog setOnMRUListener(OnMRUListener onMRUListener) {
        this.onMRUListener = onMRUListener;
        return this;
    }

    public String getItem(int position) {
        return adapter.getItem(position);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setView(setupUI())
                .setTitle(getResources().getString(R.string.recently_used_tags))
                .setNegativeButton(getResources().getString(R.string.close), null)
                .create();
    }

    private View setupUI() {
        @SuppressLint("InflateParams") View view = getActivity().getLayoutInflater().inflate(R.layout.mru_list, null);
        initRecyclerView(view);

        return view;
    }

    private void initRecyclerView(View rootView) {
        adapter = new MRUAdapter(this, getArguments().getStringArrayList(ARG_MRU_LIST));
        adapter.setOnMRUListener(onMRUListener);

        RecyclerView recyclerView = rootView.findViewById(R.id.list);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter);
        addSwipeToDelete(recyclerView);
    }

    private void addSwipeToDelete(RecyclerView recyclerView) {
        @SuppressWarnings("ConstantConditions") final SwipeToDeleteCallback swipeHandler = new SwipeToDeleteCallback(getActivity(),
                ContextCompat.getDrawable(getActivity(), R.drawable.ic_action_delete),
                new ColorDrawable(ContextCompat.getColor(getActivity(), R.color.animation_delete_bg))) {
            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                adapter.removeAt(viewHolder.getAdapterPosition());
            }
        };

        new ItemTouchHelper(swipeHandler).attachToRecyclerView(recyclerView);
    }
}
