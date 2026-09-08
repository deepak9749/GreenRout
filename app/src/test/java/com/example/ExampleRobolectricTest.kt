package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.KawadiwalaRequest
import com.example.data.model.WasteAvailability
import com.example.domain.model.AvailabilityStatus
import com.example.domain.model.RequestStatus
import com.example.domain.model.UserRole
import com.example.domain.model.WasteType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.flow.first
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Green Route", appName)
    }

    @Test
    fun `verify waste availability strict 24 hour expiry rule`() {
        val now = 1000000000L
        val dayDuration = 24L * 60L * 60L * 1000L

        val availability = WasteAvailability(
            id = "test_avail_1",
            householdId = "h1",
            householdName = "Ramesh",
            availableAt = now,
            expiresAt = now + dayDuration,
            status = AvailabilityStatus.AVAILABLE
        )

        assertEquals(now + dayDuration, availability.expiresAt)
        assertEquals(dayDuration, availability.expiresAt - availability.availableAt)
    }

    @Test
    fun `verify waste type enum keys`() {
        assertEquals("Plastic", WasteType.PLASTIC.key)
        assertEquals("Iron", WasteType.IRON.key)
        assertEquals("Glass", WasteType.GLASS.key)
        assertEquals("Metal", WasteType.METAL.key)
        assertEquals("Fridge", WasteType.FRIDGE.key)
        assertEquals("TV", WasteType.TV.key)
        assertEquals("Electronics", WasteType.ELECTRONICS.key)
        assertEquals("Paper", WasteType.PAPER.key)
    }

    @Test
    fun `verify user roles`() {
        assertEquals(UserRole.HOUSEHOLD, UserRole.fromString("household"))
        assertEquals(UserRole.KAWADIWALA, UserRole.fromString("kawadiwala"))
    }

    @Test
    fun `verify all five pre-configured dummy accounts login with correct roles and credentials`() = kotlinx.coroutines.runBlocking {
        val authRepo = com.example.data.repository.AuthRepository()

        // 1. Mandal Bhavan (Household)
        val mandalResult = authRepo.login("mandal.bhavan.demo@greenroute.app", "GreenRoute@2026")
        assertTrue("Mandal Bhavan login must succeed", mandalResult.isSuccess)
        val mandalUser = mandalResult.getOrThrow()
        assertEquals("Mandal Bhavan", mandalUser.name)
        assertEquals("mandal.bhavan.demo@greenroute.app", mandalUser.email)
        assertEquals(UserRole.HOUSEHOLD, mandalUser.role)
        assertTrue(mandalUser.isDemoUser)
        authRepo.logout()

        // 2. Santi Kunja (Household)
        val santiResult = authRepo.login("santi.kunja.demo@greenroute.app", "GreenRoute@2026")
        assertTrue("Santi Kunja login must succeed", santiResult.isSuccess)
        val santiUser = santiResult.getOrThrow()
        assertEquals("Santi Kunja", santiUser.name)
        assertEquals("santi.kunja.demo@greenroute.app", santiUser.email)
        assertEquals(UserRole.HOUSEHOLD, santiUser.role)
        assertTrue(santiUser.isDemoUser)
        authRepo.logout()

        // 3. Imran (Kawadiwala)
        val imranResult = authRepo.login("imran.demo@greenroute.app", "GreenRoute@2026")
        assertTrue("Imran login must succeed", imranResult.isSuccess)
        val imranUser = imranResult.getOrThrow()
        assertEquals("Imran", imranUser.name)
        assertEquals("imran.demo@greenroute.app", imranUser.email)
        assertEquals(UserRole.KAWADIWALA, imranUser.role)
        assertTrue(imranUser.isDemoUser)
        authRepo.logout()

        // 4. Ramesh (Kawadiwala)
        val rameshResult = authRepo.login("ramesh.demo@greenroute.app", "GreenRoute@2026")
        assertTrue("Ramesh login must succeed", rameshResult.isSuccess)
        val rameshUser = rameshResult.getOrThrow()
        assertEquals("Ramesh", rameshUser.name)
        assertEquals("ramesh.demo@greenroute.app", rameshUser.email)
        assertEquals(UserRole.KAWADIWALA, rameshUser.role)
        assertTrue(rameshUser.isDemoUser)
        authRepo.logout()

        // 5. Suresh (Kawadiwala)
        val sureshResult = authRepo.login("suresh.demo@greenroute.app", "GreenRoute@2026")
        assertTrue("Suresh login must succeed", sureshResult.isSuccess)
        val sureshUser = sureshResult.getOrThrow()
        assertEquals("Suresh", sureshUser.name)
        assertEquals("suresh.demo@greenroute.app", sureshUser.email)
        assertEquals(UserRole.KAWADIWALA, sureshUser.role)
        assertTrue(sureshUser.isDemoUser)
        authRepo.logout()
    }

    @Test
    fun `verify booked location access control strictly allows only accepted kawadiwala`() = kotlinx.coroutines.runBlocking {
        val wasteRepo = com.example.data.repository.WasteRepository()

        // Imran is the accepted Kawadiwala for demo_booked_mandal
        val imranAccess = wasteRepo.getBookedPickupForKawadiwala(
            availabilityId = "demo_booked_mandal",
            kawadiwalaId = "demo_imran"
        )
        assertTrue("Imran should be authorized to track his booked Mandal Bhavan pickup", imranAccess.isSuccess)
        val bookedPickup = imranAccess.getOrThrow()
        assertEquals("Mandal Bhavan", bookedPickup.householdName)
        assertEquals(AvailabilityStatus.BOOKED, bookedPickup.status)
        assertEquals("demo_imran", bookedPickup.acceptedKawadiwalaId)

        // Ramesh is NOT the accepted Kawadiwala for demo_booked_mandal
        val rameshAccess = wasteRepo.getBookedPickupForKawadiwala(
            availabilityId = "demo_booked_mandal",
            kawadiwalaId = "demo_ramesh"
        )
        assertFalse("Ramesh must NOT have access to Mandal Bhavan pickup", rameshAccess.isSuccess)
        assertTrue("Exception should be SecurityException", rameshAccess.exceptionOrNull() is SecurityException)

        // Suresh is NOT the accepted Kawadiwala for demo_booked_mandal
        val sureshAccess = wasteRepo.getBookedPickupForKawadiwala(
            availabilityId = "demo_booked_mandal",
            kawadiwalaId = "demo_suresh"
        )
        assertFalse("Suresh must NOT have access to Mandal Bhavan pickup", sureshAccess.isSuccess)
        assertTrue("Exception should be SecurityException", sureshAccess.exceptionOrNull() is SecurityException)
    }

    @Test
    fun `verify RouteRepository returns road geometry, distance, and duration`() = kotlinx.coroutines.runBlocking {
        val routeRepo = com.example.data.repository.RouteRepository()
        val origin = com.google.android.gms.maps.model.LatLng(22.438500, 88.412500)
        val destination = com.google.android.gms.maps.model.LatLng(22.443500, 88.416800) // Mandal Bhavan

        val result = routeRepo.getRoute(origin, destination, "Mandal Bhavan")
        assertTrue("Route calculation must succeed", result.isSuccess)

        val route = result.getOrThrow()
        assertTrue("Route must contain multiple coordinate waypoints for real road path", route.points.size >= 2)
        assertTrue("Distance meters must be positive", route.distanceMeters > 0)
        assertTrue("Duration seconds must be positive", route.durationSeconds > 0)
        assertTrue("Distance text must be formatted", route.distanceText.isNotEmpty())
        assertTrue("Duration text must be formatted", route.durationText.isNotEmpty())
    }

    @Test
    fun `verify only accepted Kawadiwala sees booked location on their map`() = kotlinx.coroutines.runBlocking {
        val wasteRepo = com.example.data.repository.WasteRepository()

        // 1. Imran should see demo_booked_mandal and demo_booked_santi
        val imranList = wasteRepo.getActiveAvailabilitiesForKawadiwala("demo_imran").first()
        assertTrue("Imran should see Mandal Bhavan on map", imranList.any { it.id == "demo_booked_mandal" })
        assertTrue("Imran should see Santi Kunja on map", imranList.any { it.id == "demo_booked_santi" })

        // 2. Ramesh must NOT see demo_booked_mandal or demo_booked_santi
        val rameshList = wasteRepo.getActiveAvailabilitiesForKawadiwala("demo_ramesh").first()
        assertFalse("Ramesh must NOT see Mandal Bhavan on map", rameshList.any { it.id == "demo_booked_mandal" })
        assertFalse("Ramesh must NOT see Santi Kunja on map", rameshList.any { it.id == "demo_booked_santi" })

        // 3. Suresh must NOT see demo_booked_mandal or demo_booked_santi
        val sureshList = wasteRepo.getActiveAvailabilitiesForKawadiwala("demo_suresh").first()
        assertFalse("Suresh must NOT see Mandal Bhavan on map", sureshList.any { it.id == "demo_booked_mandal" })
        assertFalse("Suresh must NOT see Santi Kunja on map", sureshList.any { it.id == "demo_booked_santi" })
    }

    @Test
    fun `verify theme preferences persistence and toggle`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val themePrefs = com.example.data.local.ThemePreferences(context)

        // Default should be LIGHT
        themePrefs.setThemeMode(com.example.data.local.AppThemeMode.LIGHT)
        assertEquals(com.example.data.local.AppThemeMode.LIGHT, themePrefs.themeMode.value)
        assertFalse(themePrefs.isDarkTheme)

        // Toggle to DARK
        themePrefs.toggleTheme()
        assertEquals(com.example.data.local.AppThemeMode.DARK, themePrefs.themeMode.value)
        assertTrue(themePrefs.isDarkTheme)

        // New instance reading from SharedPreferences
        val themePrefsReloaded = com.example.data.local.ThemePreferences(context)
        assertEquals(com.example.data.local.AppThemeMode.DARK, themePrefsReloaded.themeMode.value)
        assertTrue(themePrefsReloaded.isDarkTheme)

        // Toggle back to LIGHT
        themePrefsReloaded.toggleTheme()
        assertEquals(com.example.data.local.AppThemeMode.LIGHT, themePrefsReloaded.themeMode.value)
        assertFalse(themePrefsReloaded.isDarkTheme)
    }

    @Test
    fun `verify household can remove active availability and unauthorized household cannot`() = kotlinx.coroutines.runBlocking {
        val wasteRepo = com.example.data.repository.WasteRepository()

        // Create an availability for household "h_test_1"
        val created = wasteRepo.createAvailability(
            householdId = "h_test_1",
            householdName = "Test Household",
            wasteTypes = listOf("Plastic"),
            perWasteQuantity = mapOf("Plastic" to 10.0),
            photoUrl = null,
            lat = 22.4400,
            lng = 88.4150,
            locationType = "CUSTOM"
        ).getOrThrow()

        assertEquals("CUSTOM", created.locationType)
        assertEquals(AvailabilityStatus.AVAILABLE, created.status)

        // Another household tries to remove it -> must fail
        val unauthorizedResult = wasteRepo.removeAvailability(created.id, "h_intruder")
        assertTrue("Intruder must NOT be able to remove another household's availability", unauthorizedResult.isFailure)

        // Owner removes it -> must succeed
        val authorizedResult = wasteRepo.removeAvailability(created.id, "h_test_1")
        assertTrue("Owner should be able to remove their availability", authorizedResult.isSuccess)

        // Verify status is REMOVED
        val removed = authorizedResult.getOrThrow()
        assertEquals(AvailabilityStatus.REMOVED, removed.status)
    }

    @Test
    fun `verify request cancellation on availability removal`() = kotlinx.coroutines.runBlocking {
        val wasteRepo = com.example.data.repository.WasteRepository()
        val notifRepo = com.example.data.repository.NotificationRepository()
        val requestRepo = com.example.data.repository.RequestRepository(wasteRepo, notifRepo)

        // Submit request
        val reqResult = requestRepo.submitRequest(
            wasteAvailabilityId = "demo_loc_1",
            kawadiwalaId = "k_cancel_test",
            kawadiwalaName = "Suresh",
            offersPerWaste = mapOf("Plastic" to 150.0),
            estimatedArrivalHours = 1
        )
        assertTrue(reqResult.isSuccess)
        val req = reqResult.getOrThrow()

        // Cancel requests for this availability
        requestRepo.cancelRequestsForAvailability("demo_loc_1")

        val updatedRequests = requestRepo.getRequestsForAvailability("demo_loc_1").first()
        val updatedReq = updatedRequests.find { it.id == req.id }
        assertEquals(RequestStatus.CANCELLED, updatedReq?.status)
    }
}

