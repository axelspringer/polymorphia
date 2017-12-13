package de.bild.codec;

import de.bild.codec.annotations.PostLoad;
import de.bild.codec.annotations.PreSave;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

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



    static class TestPojo {
        public <T> Map<String,T> getAttributesOfType(T argument) {
            return null;
        }

        @PostLoad
        protected void init() {

        }

        @PreSave
        private void preSave() {

        }

    }

    @Test
    public void getDeclaredAndInheritedMethodsTest() throws Exception {
        List<MethodTypePair> declaredAndInheritedMethods = ReflectionHelper.getDeclaredAndInheritedMethods(TestPojo.class);
        assertEquals(3, declaredAndInheritedMethods.size());
        assertThat(declaredAndInheritedMethods, IsIterableContainingInAnyOrder.containsInAnyOrder(
                new MethodTypeBaseMatcher(TestPojo.class.getDeclaredMethod("getAttributesOfType", Object.class)),
                new MethodTypeBaseMatcher(TestPojo.class.getDeclaredMethod("init")),
                new MethodTypeBaseMatcher(TestPojo.class.getDeclaredMethod("preSave"))));
    }

    class MethodTypeBaseMatcher extends BaseMatcher<MethodTypePair> {
        Method method;

        public MethodTypeBaseMatcher(Method method) {
            this.method = method;
        }

        @Override
        public boolean matches(Object o) {
            if (o == null || MethodTypePair.class != o.getClass()) return false;

            MethodTypePair that = (MethodTypePair) o;

            return method.equals(that.getMethod());
        }

        @Override
        public void describeTo(Description description) {

        }
    }
}