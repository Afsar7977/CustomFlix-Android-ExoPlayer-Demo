@file:Suppress("PackageName", "unused")

package com.afsar.customflix.Utilities

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.afsar.customflix.Modal.Downloads
import java.util.*

class DBHelper(context: Context?) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    // Creating Tables
    override fun onCreate(db: SQLiteDatabase) {
        // create notes table
        Log.d("create_table", "::" + Downloads.CREATE_TABLE)
        db.execSQL(Downloads.CREATE_TABLE)
    }

    // Upgrading database
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + Downloads.TABLE_NAME)
        // Create tables again
        onCreate(db)
    }

    fun insertDownloads(
        name: String?,
        type: String?,
        path: String?,
        duration: String?,
        image: String?
    ): Long {
        // get writable database as we want to write data
        val db = this.writableDatabase
        val values = ContentValues()
        // `id` and `timestamp` will be inserted automatically.
        // no need to add them
        values.put(Downloads.COLUMN_NAME, name)
        values.put(Downloads.COLUMN_PATH, path)
        values.put(Downloads.COLOUMN_DURATION, duration)
        values.put(Downloads.COLUMN_TYPE, type)
        values.put(Downloads.COLUMN_IMAGE, image)

        // insert row
        val id = db.insert(Downloads.TABLE_NAME, null, values)

        // close db connection
        db.close()

        // return newly inserted row id
        return id
    }

    @SuppressLint("Recycle")
    fun checkDownloads(path: String): Boolean {
        // get writable database as we want to write data
        val db = this.writableDatabase
        val isAvailable: Boolean
        val selectQuery = "SELECT * FROM " + Downloads.TABLE_NAME + " WHERE " +
                Downloads.COLUMN_PATH + " = '" + path + "'"
        Log.d("query", "" + selectQuery)
        val cursor = db.rawQuery(selectQuery, null)

        // looping through all rows and adding to list
        Log.d("query", "" + cursor.count)
        isAvailable = when {
            cursor.count > 0 -> {
                true
            }
            else -> {
                false
            }
        }
        // close db connection
        db.close()
        // return newly inserted row id
        return isAvailable
    }

    fun getDownloads(id: Long): Downloads {
        // get readable database as we are not inserting anything
        val db = this.readableDatabase
        val cursor = db.query(
            Downloads.TABLE_NAME,
            arrayOf(
                Downloads.COLUMN_ID,
                Downloads.COLUMN_NAME,
                Downloads.COLUMN_TYPE,
                Downloads.COLUMN_PATH,
                Downloads.COLOUMN_DURATION,
                Downloads.COLUMN_IMAGE
            ),
            Downloads.COLUMN_ID + "=?",
            arrayOf(id.toString()),
            null,
            null,
            null,
            null
        )
        cursor?.moveToFirst()

        // prepare note object
        val note = Downloads(
            cursor!!.getInt(cursor.getColumnIndex(Downloads.COLUMN_ID)),
            cursor.getString(cursor.getColumnIndex(Downloads.COLUMN_NAME)),
            cursor.getString(cursor.getColumnIndex(Downloads.COLUMN_TYPE)),
            cursor.getString(cursor.getColumnIndex(Downloads.COLUMN_PATH)),
            cursor.getString(cursor.getColumnIndex(Downloads.COLOUMN_DURATION)),
            cursor.getString(cursor.getColumnIndex(Downloads.COLUMN_IMAGE))
        )

        // close the db connection
        cursor.close()
        return note
    }

    // Select All Query
    val allDownloads: List<Downloads>
        @SuppressLint("Recycle")
        get() {
            // looping through all rows and adding to list
            // close db connection
            // return notes list
            val notes: MutableList<Downloads> = ArrayList()

            // Select All Query
            val selectQuery = "SELECT  * FROM " + Downloads.TABLE_NAME
            val db = this.writableDatabase
            val cursor = db.rawQuery(selectQuery, null)

            // looping through all rows and adding to list
            when {
                cursor.moveToFirst() -> do {
                    val note = Downloads()
                    note.id = cursor.getInt(cursor.getColumnIndex(Downloads.COLUMN_ID))
                    note.name = cursor.getString(cursor.getColumnIndex(Downloads.COLUMN_NAME))
                    note.type = cursor.getString(cursor.getColumnIndex(Downloads.COLUMN_TYPE))
                    note.path = cursor.getString(cursor.getColumnIndex(Downloads.COLUMN_PATH))
                    note.duration =
                        cursor.getString(cursor.getColumnIndex(Downloads.COLOUMN_DURATION))
                    note.image = cursor.getString(cursor.getColumnIndex(Downloads.COLUMN_IMAGE))
                    notes.add(note)
                } while (cursor.moveToNext())
            }

            // close db connection
            db.close()

            // return notes list
            return notes
        }

    // return count
    val downloadsCount: Int
        get() {
            val countQuery = "SELECT  * FROM " + Downloads.TABLE_NAME
            val db = this.readableDatabase
            val cursor = db.rawQuery(countQuery, null)
            val count = cursor.count
            cursor.close()
            // return count
            return count
        }

    fun updateDownloads(note: Downloads): Int {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(Downloads.COLUMN_NAME, note.name)
        values.put(Downloads.COLUMN_PATH, note.path)
        values.put(Downloads.COLOUMN_DURATION, note.path)
        values.put(Downloads.COLUMN_TYPE, note.type)
        values.put(Downloads.COLUMN_IMAGE, note.image)

        // updating row
        return db.update(
            Downloads.TABLE_NAME,
            values,
            Downloads.COLUMN_ID + " = ?",
            arrayOf(java.lang.String.valueOf(note.id))
        )
    }

    fun deleteDownloads(note: Downloads) {
        val db = this.writableDatabase
        db.delete(
            Downloads.TABLE_NAME,
            Downloads.COLUMN_ID + " = ?",
            arrayOf(java.lang.String.valueOf(note.id))
        )
        db.close()
    }

    fun deleteDownloadsByPath(path: String) {
        Log.d("deleteDownloads", "::$path")
        val db = this.writableDatabase
        db.delete(
            Downloads.TABLE_NAME, Downloads.COLUMN_PATH + " = ?", arrayOf(
                path
            )
        )
        db.close()
    }

    companion object {
        // Database Version
        private const val DATABASE_VERSION = 1

        // Database Name
        private const val DATABASE_NAME = "downloads_db"
    }
}