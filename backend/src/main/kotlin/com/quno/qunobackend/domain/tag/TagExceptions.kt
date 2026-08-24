package com.quno.qunobackend.domain.tag

class TagNotFoundException(id: Long) : RuntimeException("Tag not found: $id")
