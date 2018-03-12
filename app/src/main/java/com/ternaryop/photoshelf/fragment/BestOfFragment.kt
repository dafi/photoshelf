package com.ternaryop.photoshelf.fragment

import android.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridView
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.adapter.photo.GridViewPhotoAdapter

class BestOfFragment : Fragment() {

    private lateinit var gridView: GridView
    private var gridViewPhotoAdapter: GridViewPhotoAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_best_of, container, false)

        gridViewPhotoAdapter = GridViewPhotoAdapter(activity, LOADER_PREFIX)

//        gridView = rootView.findViewById<View>(R.id.gridview) as GridView
        //        gridView.setAdapter(gridViewPhotoAdapter);
        //        gridView.setOnItemClickListener(this);
//        gridView.setMultiChoiceModeListener(this)

        rootView.findViewById<View>(R.id.create_post).setOnClickListener { }

        return rootView
    }

    companion object {
        private const val PICK_IMAGE_REQUEST_CODE = 100
        private const val LOADER_PREFIX = "mediumThumb"
    }
}
