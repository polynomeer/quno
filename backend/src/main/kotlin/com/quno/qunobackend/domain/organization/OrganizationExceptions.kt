package com.quno.qunobackend.domain.organization

class OrganizationNotFoundException(id: Long) : RuntimeException("Organization not found: $id")

/** Thrown when a name collides on [Organization.slugify] with an existing organization —
 * mirrors Tag's uq_tags_slug_active, but as a create-time 409 rather than silent find-or-create,
 * since creating an organization is a deliberate user action (not tagging a post). */
class DuplicateOrganizationNameException(name: String) : RuntimeException("Organization name already taken: $name")
