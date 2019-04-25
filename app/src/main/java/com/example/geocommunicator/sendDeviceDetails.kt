package com.example.geocommunicator

import android.os.AsyncTask
import android.util.Log
import java.io.DataOutputStream
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL

// Update Database
class SendDeviceDetails : AsyncTask<String, Void, String>() {

    private var TAG = "Asynctask"

    override fun doInBackground(vararg params: String?): String {
        var data = ""
        if (!isCancelled) {
            try {
                // Debug
                Log.d(TAG, "URL: " + params[0].toString())
                Log.d(TAG, "Data: " + params[1].toString())

                val url: URL = URL(params[0])
                val urlConnection = url.openConnection() as HttpURLConnection


                urlConnection.requestMethod = "POST"
                urlConnection.doOutput = true

                // Need this?
                urlConnection.doInput = true

                val contentType = "application/json"
                urlConnection.setRequestProperty("Content-Type", contentType)
                urlConnection.setRequestProperty("Accept",contentType)

                val wr = DataOutputStream(urlConnection.outputStream)

                wr.writeBytes(params[1])

                wr.flush()
                wr.close()

                /*
                val ins = urlConnection.inputStream
                val inputStreamReader = InputStreamReader(ins)

                val inputStreamData = inputStreamReader.read()
                while (inputStreamData != -1) {
                    val current = inputStreamData as Char
                    data += current
                }
                */

                Log.d(TAG, "Response code: " + urlConnection.responseCode.toString())
                //Log.d(TAG, "Response message: " + urlConnection.responseMessage)

            } catch (e : Exception) {
                e.printStackTrace()

            }
        } else {
            Log.d(TAG, "AsyncTask cancelled.")
        }
        // Debug
        //Log.d(TAG, "Result: " + data)
        return data
    }

    override fun onPostExecute(result: String) {
        super.onPostExecute(result)
        // this is expecting a response code to be sent from your server upon receiving the POST data
        Log.e("Post", result)
    }
}