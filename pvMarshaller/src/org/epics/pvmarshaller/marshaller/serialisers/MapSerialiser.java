package org.epics.pvmarshaller.marshaller.serialisers;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.epics.pvdata.factory.FieldFactory;
import org.epics.pvdata.pv.FieldBuilder;
import org.epics.pvdata.pv.FieldCreate;
import org.epics.pvdata.pv.PVBoolean;
import org.epics.pvdata.pv.PVByte;
import org.epics.pvdata.pv.PVDouble;
import org.epics.pvdata.pv.PVFloat;
import org.epics.pvdata.pv.PVInt;
import org.epics.pvdata.pv.PVLong;
import org.epics.pvdata.pv.PVShort;
import org.epics.pvdata.pv.PVString;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.ScalarType;
import org.epics.pvdata.pv.Structure;

/**
 * Serialises a map
 * @author Matt Taylor
 *
 */
public class MapSerialiser {
	Serialiser serialiser;
	
	/**
	 * Constructor
	 * @param serialiser
	 */
	public MapSerialiser(Serialiser serialiser) {
		this.serialiser = serialiser;
	}
	
	/**
	 * Creates a Structure from a the given map
	 * @param map The map to create the structure from
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public Structure buildStructureFromMap(Map<String, ?> map) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		FieldCreate fieldCreate = FieldFactory.getFieldCreate();
		FieldBuilder fieldBuilder = fieldCreate.createFieldBuilder();
		
		if (map != null)
		{
			for (String key : map.keySet())
			{
				Object mapValue = map.get(key);
				Class<?> mapValueType = mapValue.getClass();
				
				if (PrimitiveSerialiser.isPrimitive(mapValueType)) {
					fieldBuilder.add(key, PrimitiveSerialiser.getScalarType(mapValueType));
				} else if (ContainerSerialiser.isContainer(mapValueType)) {
					serialiser.getContainerSerialiser().addToStructureWithContainerObject(mapValueType, key, fieldBuilder, map.get(key));
				} else {
	            	Structure componentStructure = serialiser.getObjectSerialiser().buildObject(map.get(key));
	            	fieldBuilder.add(key, componentStructure);
				}
				
			}
		}
		
		Structure structure = fieldBuilder.createStructure();
		
		return structure;
	}
	
	/**
	 * Builds a structure from a map of primitives
	 * @param map The map
	 * @param scalarType The primitive type
	 * @return
	 */
	public static Structure buildStructureFromPrimitiveMap(Map<String, ?> map, ScalarType scalarType)
	{
		FieldCreate fieldCreate = FieldFactory.getFieldCreate();
		FieldBuilder fieldBuilder = fieldCreate.createFieldBuilder();
		
		if (map != null)
		{
			for (String key : map.keySet())
			{
				fieldBuilder.add(key, scalarType);
			}
		}
		Structure structure = fieldBuilder.createStructure();
		
		return structure;
	}
	
	/**
	 * Builds a structure from a map of containers
	 * @param map The map
	 * @param objectClass The container class
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public Structure buildStructureFromContainerMap(Map<String, ?> map, Class<?> objectClass) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException
	{
		FieldCreate fieldCreate = FieldFactory.getFieldCreate();
		FieldBuilder fieldBuilder = fieldCreate.createFieldBuilder();
		
		for (String key : map.keySet())
		{
			Structure structure = serialiser.getObjectSerialiser().buildObject(map.get(key));
			fieldBuilder.add(key, structure);
		}
		
		Structure structure = fieldBuilder.createStructure();
		
		return structure;
	}
	
	/**
	 * Builds a structure from a map of containers
	 * @param map The map
	 * @param objectClass The object class
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public Structure buildStructureFromObjectMap(Map<String, ?> map, Class<?> objectClass) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException
	{
		FieldCreate fieldCreate = FieldFactory.getFieldCreate();
		FieldBuilder fieldBuilder = fieldCreate.createFieldBuilder();
		
		for (String key : map.keySet())
		{
			Structure structure = serialiser.getObjectSerialiser().buildObject(map.get(key));
			fieldBuilder.add(key, structure);
		}
		
		Structure structure = fieldBuilder.createStructure();
		
		return structure;
	}
	
	/**
	 * Populates a PVStructure with values from a map
	 * @param name Name of the field in the PVStrucutre to populate
	 * @param parentStructure The PVStructure to populate
	 * @param map The map to get data from
	 * @throws Exception
	 */
	public void setMapValues(String name, PVStructure parentStructure, Map<String, ?> map) throws Exception
	{
		if (map != null)
		{
			for (String key : map.keySet())
			{
				Object mapValue = map.get(key);
				Class<?> mapValueType = mapValue.getClass();
				
				PVStructure mapPVStructure = parentStructure.getStructureField(name);
				
				if (PrimitiveSerialiser.isPrimitive(mapValueType)) {
					setPrimitiveMapValue(mapPVStructure, key, mapValue, mapValueType);
				} else if (ContainerSerialiser.isContainer(mapValueType)) {
					setContainerMapValue(mapPVStructure, key, mapValue, mapValueType);
				} else {
					setObjectMapValue(mapPVStructure, key, mapValue, mapValueType);
				}
			}
		}
	}
	
	/**
	 * Populates a PVStructure with values from a map
	 * @param mapPVStructure The PVStructure to populate
	 * @param map The map to get data from
	 * @throws Exception
	 */
	public void setMapValues(PVStructure mapPVStructure, Map<String, ?> map) throws Exception
	{
		if (map != null)
		{
			for (String key : map.keySet())
			{
				Object mapValue = map.get(key);
				Class<?> mapValueType = mapValue.getClass();
				
				if (PrimitiveSerialiser.isPrimitive(mapValueType)) {
					setPrimitiveMapValue(mapPVStructure, key, mapValue, mapValueType);
				} else if (ContainerSerialiser.isContainer(mapValueType)) {
					setContainerMapValue(mapPVStructure, key, mapValue, mapValueType);
				} else {
					setObjectMapValue(mapPVStructure, key, mapValue, mapValueType);
				}
			}
		}
	}
	
	/**
	 * Populates data in a PVStructure representing a map with a primitive value
	 * @param mapPVStructure The PVStructure to populate
	 * @param key The field in the structure to populate
	 * @param object The primitive value
	 * @param componentType The type of primitive
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public static void setPrimitiveMapValue(PVStructure mapPVStructure, String key, Object object, Class<?> componentType) throws IllegalArgumentException, IllegalAccessException
	{    	
		if (object != null)
		{
			if (componentType.equals(Integer.class) || componentType.equals(int.class)) {
				PVInt pvValue = mapPVStructure.getSubField(PVInt.class, key);
				pvValue.put((int)object);
			} else if (componentType.equals(Short.class) || componentType.equals(short.class)) {
				PVShort pvValue = mapPVStructure.getSubField(PVShort.class, key);
				pvValue.put((short)object);
			} else if (componentType.equals(Long.class) || componentType.equals(long.class)) {
				PVLong pvValue = mapPVStructure.getSubField(PVLong.class, key);
				pvValue.put((long)object);
			} else if (componentType.equals(Byte.class) || componentType.equals(byte.class)) {
				PVByte pvValue = mapPVStructure.getSubField(PVByte.class, key);
				pvValue.put((byte)object);
			} else if (componentType.equals(Boolean.class) || componentType.equals(boolean.class)) {
				PVBoolean pvValue = mapPVStructure.getSubField(PVBoolean.class, key);
				pvValue.put((boolean)object);
			} else if (componentType.equals(Float.class) || componentType.equals(float.class)) {
				PVFloat pvValue = mapPVStructure.getSubField(PVFloat.class, key);
				pvValue.put((float)object);
			} else if (componentType.equals(Double.class) || componentType.equals(double.class)) {
				PVDouble pvValue = mapPVStructure.getSubField(PVDouble.class, key);
				pvValue.put((double)object);
			} else if (componentType.equals(Character.class) || componentType.equals(char.class)) {
				PVString pvValue = mapPVStructure.getSubField(PVString.class, key);
				pvValue.put(String.valueOf(object));
			} else if (componentType.equals(String.class)) {
				PVString pvValue = mapPVStructure.getSubField(PVString.class, key);
				pvValue.put((String)object);
			}
		}
	}
	
	/**
	 * Populates data in a PVStructure representing a map with a container value
	 * @param mapPVStructure The PVStructure to populate
	 * @param key The field in the structure to populate
	 * @param containerObject The container object
	 * @param componentType The type of container
	 * @throws Exception
	 */
	public void setContainerMapValue(PVStructure mapPVStructure, String key, Object containerObject, Class<?> containerType) throws Exception
	{
		if (containerObject != null)
		{
			serialiser.getContainerSerialiser().setFieldValueWithContainerObject(key, mapPVStructure, containerObject, containerType);
		}
	}
	
	/**
	 * Populates data in a PVStructure representing a map with a map value
	 * @param mapPVStructure The PVStructure to populate
	 * @param key The field in the structure to populate
	 * @param object The map object
	 * @param componentType The type of container
	 * @throws Exception
	 */
	public void setObjectMapValue(PVStructure mapPVStructure, String key, Object object, Class<?> componentType) throws Exception
	{
		if (object != null)
		{
			PVStructure valuePVStructure = mapPVStructure.getStructureField(key);
			serialiser.getObjectSerialiser().setValues(object, valuePVStructure);
		}
	}
}
