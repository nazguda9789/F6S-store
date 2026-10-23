package com.web.service;

import com.web.entity.ShoeAttribute;
import com.web.repository.ShoeAttributeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Optional;

class ShoeAttributeServiceTests {
    ShoeAttributeRepository repository;
    ShoeAttributeService service;
    @BeforeEach void setup() { repository = mock(ShoeAttributeRepository.class); service = new ShoeAttributeService(repository); }
    ShoeAttribute attribute(boolean active) {
        ShoeAttribute value = new ShoeAttribute(); value.setId(5L); value.setKind(ShoeAttribute.Kind.BRAND); value.setActive(active); return value;
    }
    @Test void trimsAndNormalizesName() {
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        ShoeAttribute saved = service.save(null, ShoeAttribute.Kind.BRAND, "  Test   Brand ", true);
        assertEquals("Test Brand", saved.getName()); assertTrue(saved.isActive());
    }
    @Test void rejectsBlankAndTooLong() {
        assertThrows(IllegalArgumentException.class, () -> service.save(null, ShoeAttribute.Kind.SOLE, "  ", true));
        assertThrows(IllegalArgumentException.class, () -> service.save(null, ShoeAttribute.Kind.SOLE, "a".repeat(101), true));
        verify(repository, never()).save(any());
    }
    @Test void rejectsDuplicate() {
        when(repository.existsByKindAndNameIgnoreCaseAndIdNot(ShoeAttribute.Kind.BRAND, "Nike", -1L)).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.save(null, ShoeAttribute.Kind.BRAND, "Nike", true));
    }
    @Test void cannotChangeKind() {
        when(repository.findById(5L)).thenReturn(Optional.of(attribute(true)));
        assertThrows(IllegalArgumentException.class, () -> service.save(5L, ShoeAttribute.Kind.MATERIAL, "New", true));
    }
    @Test void optionalReferenceRemainsNull() { assertNull(service.validate(null, ShoeAttribute.Kind.BRAND, null)); }
    @Test void rejectsMissingOrWrongKind() {
        assertThrows(IllegalArgumentException.class, () -> service.validate(8L, ShoeAttribute.Kind.BRAND, null));
        when(repository.findById(5L)).thenReturn(Optional.of(attribute(true)));
        assertThrows(IllegalArgumentException.class, () -> service.validate(5L, ShoeAttribute.Kind.SOLE, null));
    }
    @Test void disabledReferenceOnlyRetainedByExistingProduct() {
        when(repository.findById(5L)).thenReturn(Optional.of(attribute(false)));
        assertThrows(IllegalArgumentException.class, () -> service.validate(5L, ShoeAttribute.Kind.BRAND, null));
        assertEquals(5L, service.validate(5L, ShoeAttribute.Kind.BRAND, 5L));
    }
    @Test void canDeactivateWithoutDeleting() {
        when(repository.findById(5L)).thenReturn(Optional.of(attribute(true)));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        assertFalse(service.save(5L, ShoeAttribute.Kind.BRAND, "Brand", false).isActive());
        verify(repository, never()).delete(any());
    }
}
