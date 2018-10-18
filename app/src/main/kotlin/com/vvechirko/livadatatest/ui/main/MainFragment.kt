package com.vvechirko.livadatatest.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import com.vvechirko.livadatatest.*
import kotlinx.android.synthetic.main.main_fragment.*

class MainFragment : Fragment() {

    lateinit var viewModel: MainViewModel
    lateinit var adapter: Adapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onViewCreated(view: View, state: Bundle?) {
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)

        adapter = Adapter().onItemClick { openFragment() }
        recyclerView.adapter = adapter

        val endlessScroll = recyclerView.endlessScroll { page -> viewModel.fetch(page) }
        endlessScroll.currentPage = viewModel.currentPage
        swipeRefresh.setOnRefreshListener { viewModel.fetch() }

        viewModel.pagingData.observeResponse(this,
            { data, page -> setData(data ?: emptyList(), page) },
            { showError(it) },
            { swipeRefresh.isRefreshing = it }
        )
    }

    private fun setData(data: List<PostEntity>, page: Int) {
        if (page == PAGE_START) adapter.setData(data) else adapter.addData(data)
    }

    private fun showError(text: String) {
        Toast.makeText(context!!, text, Toast.LENGTH_LONG).show()
    }

    private fun openFragment() {
        (activity as MainActivity).addFragment()
    }
}

typealias ItemClick = (item: PostEntity) -> Unit

class Adapter : RecyclerView.Adapter<Holder>() {

    var itemClick: ItemClick? = null
    val data: MutableList<PostEntity> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false))

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(data.get(position))
        holder.itemClick = itemClick
    }

    fun setData(list: List<PostEntity>) {
        data.clear()
        data.addAll(list)
        notifyDataSetChanged()
    }

    fun addData(list: List<PostEntity>) {
        data.addAll(list)
        notifyItemRangeInserted(data.size - list.size, list.size)
    }

    fun onItemClick(action: ItemClick): Adapter {
        itemClick = action
        return this
    }
}

class Holder(view: View) : RecyclerView.ViewHolder(view) {

    val postId = view.findViewById<TextView>(R.id.postId)
    val postTitle = view.findViewById<TextView>(R.id.postTitle)
    val postBody = view.findViewById<TextView>(R.id.postBody)

    var itemClick: ItemClick? = null

    fun bind(item: PostEntity) {
        postId.text = item.id
        postTitle.text = item.title
        postBody.text = item.body

        itemView.setOnClickListener { itemClick?.invoke(item) }
    }
}
