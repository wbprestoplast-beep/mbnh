package com.example

import com.example.model.BroadcastPriority
import com.example.model.DocCategory
import com.example.model.HospitalConstants
import com.example.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun `verify hospital user roles parsing`() {
    assertEquals(UserRole.BOSS, UserRole.fromKey("BOSS"))
    assertEquals(UserRole.DOCTOR, UserRole.fromKey("Doctor"))
    assertEquals(UserRole.NURSE, UserRole.fromKey("nurse"))
    assertEquals(UserRole.ADMINISTRATOR, UserRole.fromKey("Administrator"))
    assertEquals(UserRole.NURSE, UserRole.fromKey("UNKNOWN_ROLE"))
  }

  @Test
  fun `verify hospital wards and total beds`() {
    assertEquals(3, HospitalConstants.WARDS.size)
    val totalBeds = HospitalConstants.WARDS.sumOf { it.beds.size }
    assertEquals(18, totalBeds)
  }

  @Test
  fun `verify document category mapping`() {
    assertEquals(DocCategory.PRESCRIPTION, DocCategory.fromString("Prescription"))
    assertEquals(DocCategory.LAB_PANEL, DocCategory.fromString("Lab Panel"))
    assertEquals(DocCategory.X_RAY, DocCategory.fromString("X-Ray"))
  }

  @Test
  fun `verify broadcast priorities`() {
    assertNotNull(BroadcastPriority.CRITICAL)
    assertNotNull(BroadcastPriority.APP_UPDATE)
    assertNotNull(BroadcastPriority.HIGH)
    assertNotNull(BroadcastPriority.NORMAL)
  }
}

