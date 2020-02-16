package com.ternaryop.photoshelf.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ternaryop.photoshelf.R

class BestOfFragment : AbsPhotoShelfFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_best_of, container, false)
}
