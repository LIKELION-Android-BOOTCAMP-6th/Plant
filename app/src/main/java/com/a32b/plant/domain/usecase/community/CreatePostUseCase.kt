package com.a32b.plant.domain.usecase.community

import com.a32b.plant.domain.model.CommunityActivity
import com.a32b.plant.domain.model.Post
import com.a32b.plant.domain.model.PostAuthor
import com.a32b.plant.domain.model.StudyLog
import com.a32b.plant.domain.model.Tag
import com.a32b.plant.domain.repository.CommunityRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import javax.inject.Inject

class CreatePostUseCase @Inject constructor(
    private val repository: CommunityRepository,
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase
) {
    suspend operator fun invoke(
        isShared: Boolean,
        title: String,
        content: String?,
        studyLogs: List<StudyLog>?,
        tag: Tag
    ): Result<String> {
        val user = when (val result = ensureCurrentUserUseCase()) {
            is Result.Success -> result.data
            is Result.Failure -> return Result.Failure(result.error)
        }

        val author = PostAuthor(user.uid, user.nickname, user.profileImg)
        val newPost = if (isShared) {
            Post.createShared(author = author, title = title, studyLogs = studyLogs ?: emptyList(), tag = tag)
        } else {
            Post.createOriginal(author = author, title = title, content = content ?: "", tag = tag)
        }

        return repository.savePost(newPost, CommunityActivity.post(user.uid, title))
    }
}
