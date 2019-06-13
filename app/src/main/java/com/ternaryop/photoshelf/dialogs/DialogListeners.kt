package com.ternaryop.photoshelf.dialogs

/**
 * Created by dave on 11/06/2019.
 * Close listener
 */

interface OnCloseDialogListener<T> {
    /**
     * called when source is closed
     * @param source the source object
     * @param button the button from DialogInterface.BUTTON_xxx
     * @return true if dialog can be closed, false otherwise
     */
    fun onClose(source: T, button: Int) : Boolean
}