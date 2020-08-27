package org.wordpress.android.ui.reader.usecases

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.analytics.AnalyticsTracker.Stat.FOLLOWED_BLOG_NOTIFICATIONS_READER_MENU_OFF
import org.wordpress.android.analytics.AnalyticsTracker.Stat.FOLLOWED_BLOG_NOTIFICATIONS_READER_MENU_ON
import org.wordpress.android.datasets.ReaderBlogTableWrapper
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.store.AccountStore.AddOrDeleteSubscriptionPayload
import org.wordpress.android.fluxc.store.AccountStore.AddOrDeleteSubscriptionPayload.SubscriptionAction.DELETE
import org.wordpress.android.fluxc.store.AccountStore.AddOrDeleteSubscriptionPayload.SubscriptionAction.NEW
import org.wordpress.android.fluxc.store.AccountStore.OnSubscriptionUpdated
import org.wordpress.android.ui.reader.usecases.ReaderSiteNotificationsUseCase.SiteNotificationState.Failed.AlreadyRunning
import org.wordpress.android.ui.reader.usecases.ReaderSiteNotificationsUseCase.SiteNotificationState.Failed.NoNetwork
import org.wordpress.android.ui.reader.usecases.ReaderSiteNotificationsUseCase.SiteNotificationState.Failed.RequestFailed
import org.wordpress.android.ui.reader.usecases.ReaderSiteNotificationsUseCase.SiteNotificationState.Success
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.API
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * This class handles reader notification events.
 */
class ReaderSiteNotificationsUseCase @Inject constructor(
    private val dispatcher: Dispatcher,
    private val analyticsUtilsWrapper: AnalyticsUtilsWrapper,
    private val readerBlogTableWrapper: ReaderBlogTableWrapper,
    private val networkUtilsWrapper: NetworkUtilsWrapper
) {
    private var continuation: Continuation<Boolean>? = null

    suspend fun toggleNotification(blogId: Long): SiteNotificationState {
        if (continuation != null) {
            // Toggling notification for multiple sites in parallel isn't supported
            // as the user would lose the ability to undo the action
            return AlreadyRunning
        }
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            return NoNetwork
        }

        // We want to track the action no matter the result
        trackEvent(blogId)

        val succeeded = suspendCoroutine<Boolean> { cont ->
            continuation = cont
            updateSubscription(blogId)
        }

        return if (succeeded) {
            updateBlogInDb(blogId)
            dispatcher.dispatch(AccountActionBuilder.newFetchSubscriptionsAction())
            Success
        } else {
            RequestFailed
        }
    }

    private fun trackEvent(blogId: Long) {
        val trackingEvent = if (readerBlogTableWrapper.isNotificationsEnabled(blogId)) {
            FOLLOWED_BLOG_NOTIFICATIONS_READER_MENU_OFF
        } else {
            FOLLOWED_BLOG_NOTIFICATIONS_READER_MENU_ON
        }

        analyticsUtilsWrapper.trackWithSiteId(trackingEvent, blogId)
    }

    private fun updateBlogInDb(blogId: Long) {
        val isNotificationEnabledInDb = readerBlogTableWrapper.isNotificationsEnabled(blogId)
        readerBlogTableWrapper.setNotificationsEnabledByBlogId(
                blogId,
                !isNotificationEnabledInDb
        )
    }

    private fun updateSubscription(blogId: Long) {
        val action = if (readerBlogTableWrapper.isNotificationsEnabled(blogId)) {
            DELETE
        } else {
            NEW
        }
        val payload = AddOrDeleteSubscriptionPayload(blogId.toString(), action)
        dispatcher.dispatch(AccountActionBuilder.newUpdateSubscriptionNotificationPostAction(payload))
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @SuppressWarnings("unused")
    fun onSubscriptionUpdated(event: OnSubscriptionUpdated) {
        if (event.isError) {
            continuation?.resume(false)
            AppLog.e(
                    API,
                    ReaderSiteNotificationsUseCase::class.java.simpleName + ".onSubscriptionUpdated: " +
                            event.error.type + " - " + event.error.message
            )
        } else {
            continuation?.resume(true)
        }
        continuation = null
    }

    sealed class SiteNotificationState {
        object Success : SiteNotificationState()
        sealed class Failed : SiteNotificationState() {
            object NoNetwork : Failed()
            object RequestFailed : Failed()
            object AlreadyRunning : Failed()
        }
    }
}
