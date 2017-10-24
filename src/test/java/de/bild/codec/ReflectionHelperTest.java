package de.bild.codec;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

public class ReflectionHelperTest {

    static class ValueClass {
    }

    static class ExtendedValueClass extends ValueClass {
    }

    static class SomeOtherClass<V extends ValueClass> extends ArrayList<V> implements List<V> {
    }

    static class SomeSpecializedList extends SomeOtherClass<ExtendedValueClass> {
    }

    static class ListOfDates extends ArrayList<Date> {
    }

    static class SomeSpecialMap<K, V> extends HashMap<K, V> implements Map<K, V> {
    }

    static class SomeSpecialMapWithBounds<K extends String, V extends Date> extends HashMap<K, V> implements Map<K, V> {
    }

    static class SomeSpecialMapWithArgumentAndBound<V extends Date> extends HashMap<String, V> implements Map<String, V> {
    }

    @Test
    public void findInterfaceTest() throws Exception {
        assertEquals(TypeUtils.parameterize(List.class, Date.class), ReflectionHelper.findInterface(ListOfDates.class, List.class));
        assertEquals(TypeUtils.parameterize(List.class, ExtendedValueClass.class), ReflectionHelper.findInterface(SomeSpecializedList.class, List.class));
        //maps
        assertEquals(TypeUtils.parameterize(Map.class, Object.class, Object.class), ReflectionHelper.findInterface(SomeSpecialMap.class, Map.class));
        assertEquals(TypeUtils.parameterize(Map.class, String.class, Date.class), ReflectionHelper.findInterface(SomeSpecialMapWithBounds.class, Map.class));
        assertEquals(TypeUtils.parameterize(Map.class, String.class, Date.class), ReflectionHelper.findInterface(SomeSpecialMapWithArgumentAndBound.class, Map.class));
    }


    static List<Date> dateList;
    static ReflectionHelperTest someRawClass;

    @Test
    public void extractRawClassTest() throws Exception {
        assertEquals(List.class, ReflectionHelper.extractRawClass(ReflectionHelperTest.class.getDeclaredField("dateList").getGenericType()));
        assertEquals(ReflectionHelperTest.class, ReflectionHelper.extractRawClass(ReflectionHelperTest.class.getDeclaredField("someRawClass").getGenericType()));
    }
}