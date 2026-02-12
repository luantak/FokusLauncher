package com.lu4p.fokuslauncher.receiver

import android.content.Context
import android.content.Intent
import com.lu4p.fokuslauncher.data.repository.AppRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PackageChangeReceiverTest {

    private lateinit var context: Context
    private lateinit var appRepository: AppRepository
    private lateinit var receiver: PackageChangeReceiver

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        appRepository = mockk(relaxed = true)
        receiver = PackageChangeReceiver()
        receiver.appRepository = appRepository
    }

    @Test
    fun `onReceive with PACKAGE_ADDED calls onPackageAdded`() = runTest {
        val intent = Intent(Intent.ACTION_PACKAGE_ADDED).apply {
            data = android.net.Uri.parse("package:com.example.app")
        }

        receiver.onReceive(context, intent)
        advanceUntilIdle()

        coVerify { appRepository.onPackageAdded("com.example.app") }
    }

    @Test
    fun `onReceive with PACKAGE_REMOVED calls onPackageRemoved`() = runTest {
        val intent = Intent(Intent.ACTION_PACKAGE_REMOVED).apply {
            data = android.net.Uri.parse("package:com.example.app")
        }

        receiver.onReceive(context, intent)
        advanceUntilIdle()

        coVerify { appRepository.onPackageRemoved("com.example.app") }
    }

    @Test
    fun `onReceive with PACKAGE_CHANGED calls onPackageChanged`() = runTest {
        val intent = Intent(Intent.ACTION_PACKAGE_CHANGED).apply {
            data = android.net.Uri.parse("package:com.example.app")
        }

        receiver.onReceive(context, intent)
        advanceUntilIdle()

        coVerify { appRepository.onPackageChanged("com.example.app") }
    }

    @Test
    fun `onReceive with unrelated action does not call repository`() = runTest {
        val intent = Intent("android.intent.action.SOME_OTHER_ACTION")

        receiver.onReceive(context, intent)
        advanceUntilIdle()

        coVerify(exactly = 0) { appRepository.onPackageAdded(any()) }
        coVerify(exactly = 0) { appRepository.onPackageRemoved(any()) }
        coVerify(exactly = 0) { appRepository.onPackageChanged(any()) }
    }

    @Test
    fun `onReceive with null package name does nothing`() = runTest {
        val intent = Intent(Intent.ACTION_PACKAGE_ADDED)

        receiver.onReceive(context, intent)
        advanceUntilIdle()

        coVerify(exactly = 0) { appRepository.onPackageAdded(any()) }
    }
}
