@file:Suppress("PackageName")

package com.afsar.customflix.Modal

class Downloads {
    var id = 0
    var name: String? = null
    var type: String? = null
    var path: String? = null
    var duration: String? = null
    var image: String? = null

    constructor() {}
    constructor(
        id: Int,
        name: String?,
        type: String?,
        path: String?,
        duration: String?,
        Image: String?
    ) {
        this.id = id
        this.name = name
        this.type = type
        this.path = path
        this.duration = duration
        image = Image
    }

    companion object {
        const val TABLE_NAME = "downloads"
        const val COLUMN_ID = "id"
        const val COLUMN_NAME = "name"
        const val COLUMN_TYPE = "type"
        const val COLUMN_PATH = "path"
        const val COLOUMN_DURATION = "duration"
        const val COLUMN_IMAGE = "cover_image"

        // Create table SQL query
        const val CREATE_TABLE = ("CREATE TABLE " + TABLE_NAME + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_NAME + " TEXT,"
                + COLUMN_TYPE + " TEXT,"
                + COLUMN_PATH + " TEXT,"
                + COLOUMN_DURATION + " TEXT,"
                + COLUMN_IMAGE + " TEXT"
                + ")")
    }
}
