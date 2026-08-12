import com.progressoft.domain.Order;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    //  Helpers
    private Order createOrder(Long id, String name, double amount, String currency) {
        Order o = new Order();
        o.setId(id);
        o.setCustomerName(name);
        o.setAmount(amount);
        o.setCurrency(currency);
        return o;
    }

    private Order createTransientOrder(String name, double amount, String currency) {
        return createOrder(null, name, amount, currency);
    }

    private Order createPersistedOrder(long id, String name, double amount, String currency) {
        return createOrder(id, name, amount, currency);
    }

    //  Equal itself
    @Test
    void equals_shouldBeEqualToItself() {
        Order order = createTransientOrder("Alice", 100.0, "USD");
        assertEquals(order, order);
        assertTrue(order.equals(order));
    }

    //  Symmetric
    @Test
    void equals_shouldBeSymmetric() {
        Order o1 = createTransientOrder("Alice", 100.0, "USD");
        Order o2 = createTransientOrder("Alice", 100.0, "USD");
        assertTrue(o1.equals(o2) && o2.equals(o1));
        assertEquals(o1, o2);
    }

    //  Transitive
    @Test
    void equals_shouldBeTransitive() {
        Order o1 = createTransientOrder("Alice", 100.0, "USD");
        Order o2 = createTransientOrder("Alice", 100.0, "USD");
        Order o3 = createTransientOrder("Alice", 100.0, "USD");

        assertTrue(o1.equals(o2) && o2.equals(o3));
        assertEquals(o1, o3);
    }

    //  Consistency
    @Test
    void equals_shouldBeConsistent() {
        Order o1 = createTransientOrder("Alice", 100.0, "USD");
        Order o2 = createTransientOrder("Alice", 100.0, "USD");
        boolean first = o1.equals(o2);
        boolean second = o1.equals(o2);
        assertEquals(first, second);
        assertTrue(first);
    }

    //  Null & Different Type
    @Test
    void equals_shouldReturnFalseForNull() {
        Order order = createTransientOrder("Alice", 100.0, "USD");
        assertFalse(order.equals(null));
    }

    @Test
    void equals_shouldReturnFalseForDifferentType() {
        Order order = createTransientOrder("Alice", 100.0, "USD");
        assertFalse(order.equals("some string"));
    }

    //  hashCode Consistency
    @Test
    void hashCode_shouldBeConsistent() {
        Order order = createTransientOrder("Alice", 100.0, "USD");
        int first = order.hashCode();
        int second = order.hashCode();
        assertEquals(first, second);
    }

    @Test
    void hashCode_shouldMatchEquals() {
        Order o1 = createTransientOrder("Alice", 100.0, "USD");
        Order o2 = createTransientOrder("Alice", 100.0, "USD");
        assertTrue(o1.equals(o2));
        assertEquals(o1.hashCode(), o2.hashCode());
    }

    //  ID-based equality
    @Test
    void equals_shouldUseId_whenBothIdsAreNotNullAndEqual() {
        Order o1 = createPersistedOrder(1L, "Alice", 100.0, "USD");
        Order o2 = createPersistedOrder(1L, "Bob", 200.0, "EUR"); // different business key
        assertEquals(o1, o2);
        assertEquals(o1.hashCode(), o2.hashCode());
    }

    @Test
    void equals_shouldNotBeEqual_whenBothIdsAreNotNullAndDifferent() {
        Order o1 = createPersistedOrder(1L, "Alice", 100.0, "USD");
        Order o2 = createPersistedOrder(2L, "Alice", 100.0, "USD"); // same business key
        assertNotEquals(o1, o2);
        assertNotEquals(o1.hashCode(), o2.hashCode());
    }

    //  Null-ID fallback
    @Test
    void equals_shouldUseBusinessKey_whenBothIdsAreNull() {
        Order o1 = createTransientOrder("Alice", 100.0, "USD");
        Order o2 = createTransientOrder("Alice", 100.0, "USD");
        assertEquals(o1, o2);
        assertEquals(o1.hashCode(), o2.hashCode());
    }

    @Test
    void equals_shouldNotBeEqual_whenBothIdsAreNull_butBusinessKeyDiffers() {
        Order o1 = createTransientOrder("Alice", 100.0, "USD");
        Order o2 = createTransientOrder("Bob", 100.0, "USD");
        assertNotEquals(o1, o2);
        assertNotEquals(o1.hashCode(), o2.hashCode());
    }

    @Test
    void equals_shouldNotBeEqual_whenOneIdIsNullAndOtherIsNotNull() {
        Order transientOrder = createTransientOrder("Alice", 100.0, "USD");
        Order persistedOrder = createPersistedOrder(1L, "Alice", 100.0, "USD");
        assertNotEquals(transientOrder, persistedOrder);
        assertNotEquals(persistedOrder, transientOrder);
        assertNotEquals(transientOrder.hashCode(), persistedOrder.hashCode());
    }

    @Test
    void hashCode_shouldBeSameForTransientOrdersWithSameBusinessKey() {
        Order o1 = createTransientOrder("Alice", 100.0, "USD");
        Order o2 = createTransientOrder("Alice", 100.0, "USD");
        assertEquals(o1.hashCode(), o2.hashCode());
    }

    @Test
    void hashCode_shouldDiffer_whenBusinessKeyDiffers() {
        Order o1 = createTransientOrder("Alice", 100.0, "USD");
        Order o2 = createTransientOrder("Bob", 100.0, "USD");
        assertNotEquals(o1.hashCode(), o2.hashCode());
    }

    //  Null field handling
    @Test
    void equals_shouldHandleNullCustomerName() {
        Order o1 = createTransientOrder(null, 100.0, "USD");
        Order o2 = createTransientOrder(null, 100.0, "USD");
        assertEquals(o1, o2);
    }

    @Test
    void equals_shouldHandleNullCurrency() {
        Order o1 = createTransientOrder("Alice", 100.0, null);
        Order o2 = createTransientOrder("Alice", 100.0, null);
        assertEquals(o1, o2);
    }

    @Test
    void hashCode_shouldHandleNullFields() {
        Order order = createTransientOrder(null, 100.0, null);
        // Should not throw NullPointerException
        assertDoesNotThrow(order::hashCode);
    }

    //  toString
    @Test
    void toString_shouldContainAllFields() {
        Order order = createPersistedOrder(1L, "Alice", 100.0, "USD");
        String str = order.toString();
        assertTrue(str.contains("id=1"));
        assertTrue(str.contains("customerName='Alice'"));
        assertTrue(str.contains("amount=100.00"));
        assertTrue(str.contains("currency='USD'"));
    }

    @Test
    void toString_shouldHandleNullValuesGracefully() {
        Order order = createTransientOrder(null, 100.0, null);
        String str = order.toString();
        assertNotNull(str);
        assertTrue(str.contains("customerName='null'") || str.contains("customerName=null"));
        assertTrue(str.contains("currency='null'") || str.contains("currency=null"));
        // It should not throw.
    }

    //  Mutation safety
    @Test
    void equals_shouldChangeWhenIdIsSet() {
        Order transientOrder = createTransientOrder("Alice", 100.0, "USD");
        Order persistedOrder = createPersistedOrder(1L, "Alice", 100.0, "USD");

        // Initially not equal because ids differ (one null, one non-null)
        assertNotEquals(transientOrder, persistedOrder);

        // Set the transient order's id to match
        transientOrder.setId(1L);

        // Now they should be equal (both have same non-null id)
        assertEquals(transientOrder, persistedOrder);
        assertEquals(transientOrder.hashCode(), persistedOrder.hashCode());
    }
}
