package com.example.chouaib.homework4

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns

/**
 * Created by chouaib on 2/19/18.
 */

class DatabaseManipulator {

    object GeoMessagesContract {

        // Table contents are grouped together in an anonymous object.
        object GeoMessageEntry : BaseColumns {
            const val TABLE_NAME = "geoMessages"
            const val COLUMN_NAME_TITLE = "title"
            const val COLUMN_NAME_MESSAGE = "message"
            const val COLUMN_NAME_LAT = "lat"
            const val COLUMN_NAME_LON = "lon"

        }

        const val SQL_CREATE_ENTRIES =
                "CREATE TABLE ${GeoMessageEntry.TABLE_NAME} (" +
                        "${BaseColumns._ID} INTEGER PRIMARY KEY," +
                        "${GeoMessageEntry.COLUMN_NAME_TITLE} TEXT," +
                        "${GeoMessageEntry.COLUMN_NAME_MESSAGE} TEXT," +
                        "${GeoMessageEntry.COLUMN_NAME_LAT} TEXT," +
                        "${GeoMessageEntry.COLUMN_NAME_LON} TEXT)"

        const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS ${GeoMessageEntry.TABLE_NAME}"

    }

    class GeoMessagesDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(GeoMessagesContract.SQL_CREATE_ENTRIES)
        }
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            // This database is only a cache for online data, so its upgrade policy is
            // to simply to discard the data and start over
            db.execSQL(GeoMessagesContract.SQL_DELETE_ENTRIES)
            onCreate(db)
        }
        override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            onUpgrade(db, oldVersion, newVersion)
        }
        companion object {
            // If you change the database schema, you must increment the database version.
            val DATABASE_VERSION = 1
            val DATABASE_NAME = "geoMessages.db"
        }

    }
}

