package rys.ajaxpetproject.nats.controller.impl

import com.google.protobuf.Parser
import io.nats.client.Connection
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import rys.ajaxpetproject.commonmodels.chat.proto.Chat
import rys.ajaxpetproject.service.ChatService
import rys.ajaxpetproject.model.MongoChat
import rys.ajaxpetproject.nats.controller.NatsController
import rys.ajaxpetproject.request.chat.create.proto.ChatCreateRequest
import rys.ajaxpetproject.request.chat.create.proto.ChatCreateResponse
import rys.ajaxpetproject.subjects.ChatSubjectsV1

@Service
@Suppress("NestedBlockDepth")
class NatsChatCreationController(
    override val connection: Connection,
    private val chatService: ChatService
) : NatsController<ChatCreateRequest, ChatCreateResponse> {
    override val subject = ChatSubjectsV1.ChatRequest.CREATE

    override val parser: Parser<ChatCreateRequest> = ChatCreateRequest.parser()
    override fun handle(request: ChatCreateRequest): ChatCreateResponse = runCatching {

        val chat = request.chat

        val newChat: MongoChat = chatService.createChat(MongoChat(
            id = ObjectId(chat.id),
            name = chat.name,
            users = chat.usersList.map { ObjectId(it) }
        ))

        buildSuccessResponse(newChat)

    }.getOrElse {
        buildFailureResponse(it)
    }


    private fun buildSuccessResponse(newChat: MongoChat): ChatCreateResponse =
        ChatCreateResponse.newBuilder().apply {
            successBuilder.apply {
                this.result = Chat.newBuilder().apply {
                    id = newChat.id.toString()
                    name = newChat.name
                    newChat.users.forEach {
                        this.addUsers(it.toString())
                    }
                }.build()
            }
        }.build()

    private fun buildFailureResponse(e: Throwable): ChatCreateResponse {
        logger.error("Error while creating chat: ${e.message}", e)

        return ChatCreateResponse.newBuilder().apply {
            failureBuilder.apply {
                this.message = e.message
                this.internalErrorBuilder
            }
        }.build()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(NatsChatCreationController::class.java)
    }
}
