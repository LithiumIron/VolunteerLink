package com.example.volunteerlink.organisation.repository

import com.example.volunteerlink.organisation.create.model.CreateRoleTemplate

/**
 * Database access used by the Organisation Create Post flow.
 *
 * Step 2 reads the fixed role catalogue through this interface so the UI does
 * not depend directly on Supabase.
 */
interface CreatePostRepository {
    suspend fun loadRoleCatalogue(): List<CreateRoleTemplate>
}
