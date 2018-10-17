package com.vvechirko.livadatatest.ui.main

import androidx.lifecycle.ViewModel
import com.vvechirko.livadatatest.Interactor
import com.vvechirko.livadatatest.PAGE_START
import com.vvechirko.livadatatest.PagingResponseData
import com.vvechirko.livadatatest.PostEntity

class MainViewModel : ViewModel() {

    val pagingData = PagingResponseData<PostEntity>()

    val currentPage: Int
        get() = if (pagingData.isEmpty) PAGE_START else pagingData.currentPage

    init {
        if (pagingData.isEmpty) {
            fetch()
        }
    }

    fun fetch(page: Int = PAGE_START) {
        pagingData.from(Interactor.getPosts(page), page)
    }
}
