package org.wordpress.android.ui.posts

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.ActivityUtils

class PrepublishingTagsFragment : TagsFragment() {
    private var closeListener: PrepublishingScreenClosedListener? = null

    override fun getContentLayout() = R.layout.fragment_prepublishing_tags

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mTagsSelectedListener = parentFragment as TagsSelectedListener
        closeListener = parentFragment as PrepublishingScreenClosedListener
    }

    override fun onDetach() {
        super.onDetach()
        closeListener = null
        mTagsSelectedListener = null
    }

    companion object {
        const val TAG = "prepublishing_tags_fragment_tag"
        @JvmStatic fun newInstance(site: SiteModel, tags: String?): PrepublishingTagsFragment {
            val bundle = Bundle().apply {
                putSerializable(WordPress.SITE, site)
                putString(PostSettingsTagsActivity.KEY_TAGS, tags)
            }
            return PrepublishingTagsFragment().apply { arguments = bundle }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val closeButton = view.findViewById<ImageView>(R.id.close_button)
        val backButton = view.findViewById<ImageView>(R.id.back_button)
        val toolbarTitle = view.findViewById<TextView>(R.id.toolbar_title)

        toolbarTitle.text = context?.getString(R.string.prepublishing_nudges_toolbar_title_tags)

        closeButton.setOnClickListener { closeListener?.onCloseClicked() }
        backButton.setOnClickListener {
            ActivityUtils.hideKeyboard(requireActivity())
            closeListener?.onBackClicked()
        }
    }
}