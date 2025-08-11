package network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.domain.data.replicated.chat.ChatMessageTypeEnum
import network.bisq.mobile.domain.data.replicated.chat.CitationVO
import network.bisq.mobile.domain.data.replicated.chat.reactions.BisqEasyOpenTradeMessageReactionVO
import network.bisq.mobile.domain.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.domain.utils.DateUtils
import network.bisq.mobile.i18n.I18nSupport

class BisqEasyOpenTradeMessageModel(
    bisqEasyOpenTradeMessage: BisqEasyOpenTradeMessageDto,
    myUserProfile: UserProfileVO,
    chatReactions: List<BisqEasyOpenTradeMessageReactionVO>
) {
    val senderUserProfile: UserProfileVO = bisqEasyOpenTradeMessage.senderUserProfile
    private val myUserProfileId = myUserProfile.id

    private val _chatReactions: MutableStateFlow<List<BisqEasyOpenTradeMessageReactionVO>> = MutableStateFlow(
        chatReactions
    )
    val chatReactions: StateFlow<List<BisqEasyOpenTradeMessageReactionVO>> get() = _chatReactions.asStateFlow()

    // Delegates of BisqEasyOpenTradeMessageDto
    val id: String = bisqEasyOpenTradeMessage.messageId
    val text: String? = bisqEasyOpenTradeMessage.text
    val citation: CitationVO? = bisqEasyOpenTradeMessage.citation
    val date: Long = bisqEasyOpenTradeMessage.date
    val chatMessageType: ChatMessageTypeEnum = bisqEasyOpenTradeMessage.chatMessageType
    val tradeId: String = bisqEasyOpenTradeMessage.tradeId
    val mediator: UserProfileVO? = bisqEasyOpenTradeMessage.mediator
    val bisqEasyOffer: BisqEasyOfferVO? = bisqEasyOpenTradeMessage.bisqEasyOffer
    val citationAuthorUserName = bisqEasyOpenTradeMessage.citationAuthorUserProfile?.userName

    val textString: String = text ?: ""

    // Used for protocol log message
    var decodedText: String = text?.let { I18nSupport.decode(it) } ?: ""

    val dateString: String = DateUtils.toDateTime(date)
    val senderUserProfileId = senderUserProfile.id
    val senderUserName = senderUserProfile.userName
    val citationString: String = citation?.text ?: ""
    val isMyMessage: Boolean = senderUserProfileId == myUserProfileId

    fun isMyChatReaction(reaction: BisqEasyOpenTradeMessageReactionVO): Boolean {
        return myUserProfileId == reaction.senderUserProfile.id
    }

    fun setReactions(chatMessageReactions: List<BisqEasyOpenTradeMessageReactionVO>) {
        _chatReactions.value = chatMessageReactions
    }
}
