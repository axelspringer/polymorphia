package de.bild.codec;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.Test;

import java.lang.reflect.Type;
import java.util.*;

import static org.junit.Assert.*;

public class TypesModelTest {


    interface X<PX> {
    }

    interface XY<PXY1> extends X<PXY1> {
    }

    static class A<PA1 extends Number> implements X<PA1>, XY<PA1> {
    }

    static class AA<PAA1 extends BigInteger> extends A<PAA1> {
    }

    static class AB extends A {
    }

    static class AAA extends AA<BigInteger> {
    }

    static class AAB extends AA<BigInteger> {
    }

    static class AAAA extends AAA {
    }

    static class AX implements X<Float> {
    }

    static class AAX implements XY<Integer> {
    }

    static class ABX implements XY<BigInteger> {
    }

    static class Number {
    }

    static class Float extends Number {
    }

    static class Long extends Number {
    }

    static class Integer extends Number {
    }

    static class BigInteger extends Integer {
    }


    static class Outer {
        static class Inner {
            class InnerNonStatic {
            }

            static class InnerInner {
                class InnerInnerNonStatic {
                }
            }
        }
    }


    interface ModelInterface {}

    static class ModelClassBase implements ModelInterface {}

    static class ModelClassWithTypeParameter<T extends Number, S extends Date> extends ModelClassBase {

    }

    static class ModelClassWithoutTypeParameter extends ModelClassBase {

    }

    static class NonModelClassWithTypeParameter<F extends Float> extends ModelClassWithTypeParameter<F, Date> {

    }

    static class NonModelClassWithoutTypeParameter extends ModelClassWithoutTypeParameter {

    }

    static class NonModelClassWithoutTypeParameterExtendingModelClassWithTypeParameter extends ModelClassWithTypeParameter<BigInteger, Date> {

    }

    static class NonModelClassWithTypeParameterExtendingNonModelClass extends NonModelClassWithTypeParameter<Float> {

    }


    @Test
    public void downGradeTypeTest() {
        TypesModel typesModel = new TypesModel(new HashSet<>(
                Arrays.asList(
                        ModelClassBase.class,
                        ModelClassWithTypeParameter.class,
                        ModelClassWithoutTypeParameter.class
                )), null);

        assertThat(typesModel.getAssignableTypesWithinClassHierarchy(NonModelClassWithoutTypeParameter.class),
                IsIterableContainingInAnyOrder.containsInAnyOrder(ModelClassWithoutTypeParameter.class));
        assertThat(typesModel.getAssignableTypesWithinClassHierarchy(TypeUtils.parameterize(NonModelClassWithTypeParameter.class, Float.class)),
                IsIterableContainingInAnyOrder.containsInAnyOrder(
                        TypeUtils.parameterize(ModelClassWithTypeParameter.class, Float.class, Date.class)));
        assertThat(typesModel.getAssignableTypesWithinClassHierarchy(NonModelClassWithoutTypeParameterExtendingModelClassWithTypeParameter.class),
                IsIterableContainingInAnyOrder.containsInAnyOrder(
                        TypeUtils.parameterize(ModelClassWithTypeParameter.class, BigInteger.class, Date.class)));
        assertThat(typesModel.getAssignableTypesWithinClassHierarchy(NonModelClassWithTypeParameterExtendingNonModelClass.class),
                IsIterableContainingInAnyOrder.containsInAnyOrder(
                        TypeUtils.parameterize(ModelClassWithTypeParameter.class, Float.class, Date.class)));

    }



    static TypesModel typesModel = new TypesModel(new HashSet<>(Arrays.asList(TypesModelTest.class)), null);


    @Test
    public void classParserTest() {
        TypesModel typesModel = new TypesModel(new HashSet<>(Arrays.asList(Outer.class)), null);
        assertTrue(typesModel.allClasses.size() == 3);
        assertThat(typesModel.allClasses, IsIterableContainingInAnyOrder.containsInAnyOrder(
                Outer.class, Outer.Inner.class, Outer.Inner.InnerInner.class
        ));
    }

    @Test
    public void getClassHierarchyNodeForType() throws Exception {
        TypesModel.ClassHierarchyNode classHierarchyNodeForType = typesModel.getClassHierarchyNodeForType(A.class);
        assertNotNull(classHierarchyNodeForType);
        assertTrue(classHierarchyNodeForType.isPolymorphic());
        assertEquals(classHierarchyNodeForType.getAllConcreteChildren().size(), 6); // all

        classHierarchyNodeForType = typesModel.getClassHierarchyNodeForType(AB.class);
        assertNotNull(classHierarchyNodeForType);
        assertFalse(classHierarchyNodeForType.isPolymorphic());
        assertEquals(classHierarchyNodeForType.getAllConcreteChildren().size(), 1); // AB

        classHierarchyNodeForType = typesModel.getClassHierarchyNodeForType(AA.class);
        assertNotNull(classHierarchyNodeForType);
        assertTrue(classHierarchyNodeForType.isPolymorphic());
        assertEquals(classHierarchyNodeForType.getAllConcreteChildren().size(), 4); // AA, AAA, AAB, AAAA

        classHierarchyNodeForType = typesModel.getClassHierarchyNodeForType(X.class);
        assertNotNull(classHierarchyNodeForType);
        assertTrue(classHierarchyNodeForType.isPolymorphic());
        assertThat(classHierarchyNodeForType.getAllConcreteChildren(),
                IsIterableContainingInAnyOrder.containsInAnyOrder(A.class, AA.class, AB.class, AAA.class, AAB.class, AAAA.class, AAX.class, AX.class, ABX.class));


    }


    @Test
    public void getAssignableTypesWithinClassHierarchy() throws Exception {
        assertThat(typesModel.getAssignableTypesWithinClassHierarchy(TypeUtils.parameterize(A.class, Integer.class)),
                IsIterableContainingInAnyOrder.containsInAnyOrder(
                        TypeUtils.parameterize(A.class, Integer.class)));

        assertThat(typesModel.getAssignableTypesWithinClassHierarchy(TypeUtils.parameterize(XY.class, Float.class)),
                IsIterableContainingInAnyOrder.containsInAnyOrder(
                        TypeUtils.parameterize(A.class, Float.class)));

        assertThat(typesModel.getAssignableTypesWithinClassHierarchy(TypeUtils.parameterize(XY.class, Integer.class)),
                IsIterableContainingInAnyOrder.containsInAnyOrder(
                        TypeUtils.parameterize(A.class, Integer.class),
                        AAX.class));

        assertThat(typesModel.getAssignableTypesWithinClassHierarchy(TypeUtils.parameterize(XY.class, BigInteger.class)),
                IsIterableContainingInAnyOrder.containsInAnyOrder(
                        TypeUtils.parameterize(A.class, BigInteger.class),
                        TypeUtils.parameterize(AA.class, BigInteger.class),
                        AAB.class,
                        AAA.class,
                        ABX.class,
                        AAAA.class));

    }


    interface AInterface<P0> {
    }

    static class AList<P1, P2 extends Map<P1, Long>> implements AInterface<P1> {
    }


    @Test
    public void getAssignableTypesWithinClassHierarchyCraz() throws Exception {
        AInterface<Integer> aInterface = new AList<Integer, Map<Integer, Long>>();

        assertThat(typesModel.getAssignableTypesWithinClassHierarchy(TypeUtils.parameterize(AInterface.class, Integer.class)),
                IsIterableContainingInAnyOrder.containsInAnyOrder(
                        TypeUtils.parameterize(AList.class, Integer.class,
                                TypeUtils.parameterize(Map.class, Integer.class, Long.class))
                ));

    }

}