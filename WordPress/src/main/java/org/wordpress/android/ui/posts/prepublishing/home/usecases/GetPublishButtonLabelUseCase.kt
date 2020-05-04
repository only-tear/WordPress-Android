package org.wordpress.android.ui.posts.prepublishing.home.usecases

import android.text.TextUtils
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.PostUtils
import javax.inject.Inject

typealias StringResourceId = Int

class GetPublishButtonLabelUseCase @Inject constructor() {
    fun getLabel(editPostRepository: EditPostRepository): StringResourceId {
        val dateCreated = editPostRepository.dateCreated
        val status = editPostRepository.status
        return when {
            !TextUtils.isEmpty(dateCreated) -> {
                when {
                    status == PostStatus.SCHEDULED -> R.string.prepublishing_nudges_home_schedule_button
                    status == PostStatus.PUBLISHED || status == PostStatus.PRIVATE ->
                        R.string.prepublishing_nudges_home_publish_button
                    editPostRepository.isLocalDraft -> R.string.prepublishing_nudges_home_publish_button
                    PostUtils.isPublishDateInTheFuture(editPostRepository.dateCreated) ->
                        R.string.prepublishing_nudges_home_schedule_button
                    else -> R.string.prepublishing_nudges_home_publish_button
                }
            }
            else -> R.string.prepublishing_nudges_home_publish_button
        }
    }
}
