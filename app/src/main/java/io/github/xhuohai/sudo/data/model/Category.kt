package io.github.xhuohai.sudo.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CategoriesResponse(
    @SerialName("category_list")
    val categoryList: CategoryList
)

@Serializable
data class CategoryList(
    val categories: List<Category> = emptyList()
)

@Serializable
data class Category(
    val id: Int,
    val name: String,
    val slug: String,
    val color: String = "0088CC",
    @SerialName("text_color")
    val textColor: String = "FFFFFF",
    val description: String? = null,
    @SerialName("description_text")
    val descriptionText: String? = null,
    @SerialName("topic_count")
    val topicCount: Int = 0,
    @SerialName("post_count")
    val postCount: Int = 0,
    val position: Int = 0,
    @SerialName("parent_category_id")
    val parentCategoryId: Int? = null,
    @SerialName("subcategory_ids")
    val subcategoryIds: List<Int> = emptyList(),
    @SerialName("uploaded_logo")
    val uploadedLogo: UploadedImage? = null,
    @SerialName("uploaded_background")
    val uploadedBackground: UploadedImage? = null,
    @SerialName("read_restricted")
    val readRestricted: Boolean = false,
    @SerialName("can_edit")
    val canEdit: Boolean = false
)

@Serializable
data class UploadedImage(
    val id: Int? = null,
    val url: String? = null,
    val width: Int? = null,
    val height: Int? = null
)
