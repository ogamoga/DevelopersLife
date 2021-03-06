package com.ogamoga.developerslive.screens.item

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible
import androidx.core.widget.ContentLoadingProgressBar
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.ogamoga.developerslive.App
import com.ogamoga.developerslive.R
import com.ogamoga.developerslive.domain.model.Item
import com.ogamoga.developerslive.domain.model.ItemResource
import com.ogamoga.developerslive.domain.model.SectionType
import com.ogamoga.developerslive.domain.model.Status
import org.koin.androidx.viewmodel.ext.android.viewModel

class ItemFragment : Fragment() {
    private lateinit var errorHolder: View
    private lateinit var successHolder: View
    private lateinit var progressBar: ContentLoadingProgressBar

    private lateinit var reloadButton: AppCompatTextView
    private lateinit var image: AppCompatImageView
    private lateinit var previousButton: AppCompatImageButton
    private lateinit var nextButton: AppCompatImageButton
    private lateinit var description: AppCompatTextView

    private lateinit var sectionType: SectionType
    private var currentId: Int? = null

    private val viewModel: ItemViewModel by viewModel()
    private val liveDataObserver: (t: ItemResource) -> Unit = { itemResource ->
        if (itemResource.sectionType == sectionType) {
            when (itemResource.status) {
                Status.LOADING -> {
                    setVisibility(isLoading = true)
                }
                Status.ERROR -> {
                    setVisibility(isError = true)
                }
                Status.SUCCESS -> {
                    val item: Item = itemResource.item!!
                    currentId = item.id
                    previousButton.visibility =
                        if (item.hasPrevious) View.VISIBLE else View.GONE

                    Glide.with(requireContext())
                        .load(item.url)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .listener(glideListener)
                        .into(image)

                    description.text = item.description
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment__item, container, false)
        rootView.apply {
            errorHolder = findViewById(R.id.item__error_holder)
            successHolder = findViewById(R.id.item__success_holder)
            progressBar = findViewById(R.id.item__progress_bar)
            reloadButton = findViewById(R.id.item__error_btn)
            image = findViewById(R.id.item__card__image)
            description = findViewById(R.id.item__card_description)
            previousButton = findViewById(R.id.item__previous_btn)
            nextButton = findViewById(R.id.item__next_btn)
        }
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        restoreData(savedInstanceState)

        description.movementMethod = ScrollingMovementMethod()

        loadInitialData()

        reloadButton.setOnClickListener { loadInitialData() }
        previousButton.setOnClickListener { currentId?.let { id -> viewModel.loadPrevious(id, sectionType) } }
        nextButton.setOnClickListener { currentId?.let { id -> viewModel.loadNext(id, sectionType) } }

        viewModel.itemLiveData.observe(viewLifecycleOwner, liveDataObserver)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(App.SECTION_TYPE_KEY, sectionType.ordinal)
        currentId?.let { outState.putInt(App.CURRENT_ID_KEY, it) }
    }

    private fun restoreData(savedInstanceState: Bundle?) {
        var restoredData = arguments?.getInt(App.SECTION_TYPE_KEY)
        sectionType = if (restoredData != null) {
            SectionType.values()[restoredData]
        } else {
            restoredData = savedInstanceState?.getInt(App.SECTION_TYPE_KEY)
            SectionType.values()[restoredData ?: 0]
        }

        currentId = savedInstanceState?.getInt(App.CURRENT_ID_KEY)
        if (currentId == 0) {
            currentId = null
        }
    }

    private fun loadInitialData() {
        if (currentId == null) {
            viewModel.loadLast(sectionType)
        } else {
            viewModel.loadCurrent(currentId!!, sectionType)
        }
    }

    private fun setVisibility(isSuccess: Boolean = false, isError: Boolean = false, isLoading: Boolean = false) {
        successHolder.isVisible = isSuccess
        errorHolder.isVisible = isError
        progressBar.isVisible = isLoading
    }

    private val glideListener = object : RequestListener<Drawable> {
        override fun onLoadFailed(
            e: GlideException?,
            model: Any?,
            target: Target<Drawable>?,
            isFirstResource: Boolean
        ): Boolean {
            setVisibility(isError = true)
            return false
        }

        override fun onResourceReady(
            resource: Drawable?,
            model: Any?,
            target: Target<Drawable>?,
            dataSource: DataSource?,
            isFirstResource: Boolean
        ): Boolean {
            setVisibility(isSuccess = true)
            return false
        }
    }

    companion object {
        fun newInstance(sectionTypeOrdinal: Int): ItemFragment {
            val fragment = ItemFragment()
            val args = Bundle()
            args.putInt(App.SECTION_TYPE_KEY, sectionTypeOrdinal)
            fragment.arguments = args
            return fragment
        }
    }
}