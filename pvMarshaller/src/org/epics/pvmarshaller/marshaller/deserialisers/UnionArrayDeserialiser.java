package org.epics.pvmarshaller.marshaller.deserialisers;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVScalar;
import org.epics.pvdata.pv.PVScalarArray;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.PVStructureArray;
import org.epics.pvdata.pv.PVUnion;
import org.epics.pvdata.pv.PVUnionArray;
import org.epics.pvdata.pv.UnionArrayData;

/**
 * Deserialises a Union Array
 * @author Matt Taylr
 *
 */
public class UnionArrayDeserialiser {
	Deserialiser deserialiser;
	
	/**
	 * Constructor
	 * @param deserialiser
	 */
	public UnionArrayDeserialiser(Deserialiser deserialiser) {
		this.deserialiser = deserialiser;
	}
	
	/**
	 * Populates the target object with data from a Union Array PVField
	 * @param target The target object
	 * @param fieldName The name of the field
	 * @param pvField The PVField to get the data from
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws NoSuchFieldException
	 * @throws SecurityException
	 * @throws InstantiationException
	 */
	public void deserialise(Object target, String fieldName, PVField pvField) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, InstantiationException {
		if (pvField instanceof PVUnionArray) {
			PVUnionArray unionArrayField = (PVUnionArray)pvField;
			
			PVUnion dataArray[] = new PVUnion[unionArrayField.getLength()];
			UnionArrayData unionArrayData = new UnionArrayData();
			
			int numGot = 0;
			int totalGot = 0;
			PVUnion gotArray[];
			
			while (totalGot < unionArrayField.getLength()) {
				numGot = unionArrayField.get(numGot, unionArrayField.getLength() - numGot, unionArrayData);
				totalGot += numGot;
				gotArray = unionArrayData.data;
				
				for (int i = 0; i < numGot; i++) {
					dataArray[unionArrayData.offset + i] = gotArray[i];
				}
			}
			
			Method method = deserialiser.findSetter(target, fieldName);
			
			Parameter parameters[] = method.getParameters();
			
			if (parameters[0].getType().isArray()) {
				Class<?> componentType = parameters[0].getType().getComponentType();
				System.out.println(componentType);
				
				Object newArray = Array.newInstance(componentType, dataArray.length);
				
				for (int i = 0; i < dataArray.length; i++) {
					PVUnion arrayPVUnion = dataArray[i];
					if (!arrayPVUnion.getUnion().isVariant()) {
						throw new IllegalArgumentException("Regular unions are not supported");
					}
					PVField unionValue = arrayPVUnion.get();
					if (unionValue instanceof PVScalar) {
						Object newObject = deserialiser.getScalarDeserialiser().deserialise(unionValue, componentType);
						Array.set(newArray, i, newObject);
					} else if (unionValue instanceof PVScalarArray) {
						throw new IllegalArgumentException("Union arrays of Scalar Arrays are not supported");
					} else if (unionValue instanceof PVStructure) {
						PVStructure arrayPVStructure = (PVStructure)unionValue;
						Object newObject = deserialiser.getStructureDeserialiser().createObjectFromPVStructure(arrayPVStructure, componentType);
						((Object[])newArray)[i] = newObject;
					} else if (unionValue instanceof PVStructureArray) {
						throw new IllegalArgumentException("Union arrays of Structure Arrays are not supported");
					} else {
						throw new IllegalArgumentException("Unsupported union type");
					}
				}
				
				method.invoke(target, (Object)newArray);
				
			} else if (List.class.isAssignableFrom(parameters[0].getType()) || parameters[0].getType().equals(Collection.class)) {
	            Class<?> listClass = ContainerFunctions.getListFieldClass(target, fieldName);

				List list;
				if (parameters[0].getType().isInterface()) {
					list = new LinkedList<>();
				} else {
					list = (List) parameters[0].getType().newInstance();
				}
				
				for (int i = 0; i < dataArray.length; i++) {
					PVUnion arrayPVUnion = dataArray[i];
					if (!arrayPVUnion.getUnion().isVariant()) {
						throw new IllegalArgumentException("Regular unions are not supported");
					}
					Object newObject = null;
					
					PVField unionValue = arrayPVUnion.get();
					if (unionValue instanceof PVScalar) {
						newObject = deserialiser.getScalarDeserialiser().deserialise(unionValue, listClass);
					} else if (unionValue instanceof PVScalarArray) {
						throw new IllegalArgumentException("Union arrays of Scalar Arrays are not supported");
					} else if (unionValue instanceof PVStructure) {
						PVStructure arrayPVStructure = (PVStructure)unionValue;
						if (Map.class.isAssignableFrom(listClass)) {
							Type componentType = ContainerFunctions.getMapTypeFromListComponentParent(target, fieldName);
							newObject = deserialiser.getMapDeserialiser().createMapFromPVStructure(arrayPVStructure, listClass, componentType);
						} else {
							newObject = deserialiser.getStructureDeserialiser().createObjectFromPVStructure(arrayPVStructure, listClass);
						}
					} else if (unionValue instanceof PVStructureArray) {
						throw new IllegalArgumentException("Union arrays of Structure Arrays are not supported");
					} else {
						throw new IllegalArgumentException("Unsupported union type");
					}
										
					list.add(newObject);
				}
				method.invoke(target, list);
			} else {
				throw new IllegalArgumentException("Unsupported container type");
			}
		}
	}
	
	/**
	 * Returns an object deserialised from the specified Union Array PVField
	 * @param pvField The PVField containing the Union Array data
	 * @param valueClass The type of the object to return
	 * @return The deserialised object
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws NoSuchFieldException
	 * @throws SecurityException
	 * @throws InstantiationException
	 */
	public Object deserialise(PVField pvField, Type valueClass) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, InstantiationException {
		if (pvField instanceof PVUnionArray) {
			PVUnionArray unionArrayField = (PVUnionArray)pvField;
			
			PVUnion dataArray[] = new PVUnion[unionArrayField.getLength()];
			UnionArrayData unionArrayData = new UnionArrayData();
			
			int numGot = 0;
			int totalGot = 0;
			PVUnion gotArray[];
			
			while (totalGot < unionArrayField.getLength()) {
				numGot = unionArrayField.get(numGot, unionArrayField.getLength() - numGot, unionArrayData);
				totalGot += numGot;
				gotArray = unionArrayData.data;
				
				for (int i = 0; i < numGot; i++) {
					dataArray[unionArrayData.offset + i] = gotArray[i];
				}
			}
			
			if (ContainerFunctions.isArray(valueClass)) {
				Class<?> componentType = ContainerFunctions.getComponentType(valueClass);
				System.out.println(componentType);
				
				Object newArray = Array.newInstance(componentType, dataArray.length);
				
				for (int i = 0; i < dataArray.length; i++) {
					PVUnion arrayPVUnion = dataArray[i];
					if (!arrayPVUnion.getUnion().isVariant()) {
						throw new IllegalArgumentException("Regular unions are not supported");
					}
					PVField unionValue = arrayPVUnion.get();
					if (unionValue instanceof PVScalar) {
						Object newObject = deserialiser.getScalarDeserialiser().deserialise(unionValue, componentType);
						Array.set(newArray, i, newObject);
						//throw new IllegalArgumentException("Union arrays of Scalars are not supported");
					} else if (unionValue instanceof PVScalarArray) {
						throw new IllegalArgumentException("Union arrays of Scalar Arrays are not supported");
					} else if (unionValue instanceof PVStructure) {
						PVStructure arrayPVStructure = (PVStructure)unionValue;
						Object newObject = deserialiser.getStructureDeserialiser().createObjectFromPVStructure(arrayPVStructure, componentType);
						((Object[])newArray)[i] = newObject;
					} else if (unionValue instanceof PVStructureArray) {
						throw new IllegalArgumentException("Union arrays of Structure Arrays are not supported");
					} else {
						throw new IllegalArgumentException("Unsupported union type");
					}
				}
				
				return (Object)newArray;
				
			} else if (ContainerFunctions.isList(valueClass) || valueClass.equals(Collection.class)) {
	            Class<?> listClass = ContainerFunctions.getListClass(valueClass);
	            Class<?> componentType = ContainerFunctions.getListComponentClass(valueClass);

				List list;
				if (ContainerFunctions.isInterface(listClass)) {
					list = new LinkedList<>();
				} else {
					list = (List) ((Class<?>)listClass).newInstance();
				}
				
				for (int i = 0; i < dataArray.length; i++) {
					PVUnion arrayPVUnion = dataArray[i];
					if (!arrayPVUnion.getUnion().isVariant()) {
						throw new IllegalArgumentException("Regular unions are not supported");
					}
					Object newObject = null;
					
					PVField unionValue = arrayPVUnion.get();
					if (unionValue instanceof PVScalar) {
						newObject = deserialiser.getScalarDeserialiser().deserialise(unionValue, componentType);
					} else if (unionValue instanceof PVScalarArray) {
						throw new IllegalArgumentException("Union arrays of Scalar Arrays are not supported");
					} else if (unionValue instanceof PVStructure) {
						PVStructure arrayPVStructure = (PVStructure)unionValue;
						newObject = deserialiser.getStructureDeserialiser().createObjectFromPVStructure(arrayPVStructure, componentType);
					} else if (unionValue instanceof PVStructureArray) {
						throw new IllegalArgumentException("Union arrays of Structure Arrays are not supported");
					} else {
						throw new IllegalArgumentException("Unsupported union type");
					}
										
					list.add(newObject);
				}
				return list;
			} else {
				throw new IllegalArgumentException("Unsupported container type");
			}
		}
		return null;
	}
}
