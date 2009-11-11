package org.jdesktop.application.convert;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;
import org.jdesktop.application.MnemonicTextValue;

import javax.swing.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.net.URL;


/**
 * Instances of this class maintain a registry of ResourceConverters, which are used to convert resources from one Class type
 * to another. The intended use case is to convert String resources into a number of useful Class types, but ResourceConverterS
 * could be written to convert from a non-String type to a different non-String type. The default ResourceConverters provided all
 * convert from String.
 * <p/>
 * To use, instantiate with new, call register or registerAll with the converter(s) to be registered. To obtain a converter, call
 * converterFor() and pass in the source object to be converted, and the desired converted type. If no such converter is available, returns null.
 *
 * To use the default converters provided with the framework, call addDefaultConverters after creating an instance.
 *
 * @author Rob Ross
 * @version Date: Oct 9, 2009  5:40:42 PM
 */
public class ConverterRegistry
{

    public ConverterRegistry()
    {
    }


    public void add(@NotNull ResourceConverter converter)
    {
        assertNotNull(converter, ResourceConverter.class, "converter");

        Map<Class<?>, ResourceConverter<?, ?>> destMap = registry.get(converter.getSourceClass());
        if (destMap == null)
        {
            //no entry for this source type yet
            destMap = new HashMap<Class<?>, ResourceConverter<?,?>>();
            registry.put(converter.getSourceClass(), destMap);
        }
        destMap.put(converter.getDestClass(), converter);
    }

    public void addAll(@NotNull List<ResourceConverter> converterList)
    {
        assertNotNull(converterList, List.class, "converterList");

        for (ResourceConverter resourceConverter : converterList)
        {
            add(resourceConverter);
        }
    }

    /**
     * Remove from this ConverterRegistry the ResourceConverter in the argument. If the converter is not registered, this
     * method returns false, otherwise it removes the converter and returns true.
     *
     * @param converter the ResourceConvter to remove
     * @return true if the converter was present in the registry and successfully removed. If it was not already registered,
     *         returns false
     */
    synchronized public boolean remove(@NotNull ResourceConverter converter)
    {
        assertNotNull(converter, ResourceConverter.class, "converter");
        Map<Class<?>, ResourceConverter<?, ?>> destMap = registry.get(converter.getSourceClass());
        if (destMap == null)
        {
            return false;
        }
        if (destMap.containsKey(converter.getDestClass()))
        {
            destMap.remove(converter.getDestClass());
            return true;
        }
        return false;
    }

    /**
     * @param targetType the desired Class type of the converted value
     * @return a converter that will convert a string value to the target type. Returns null if no converter cabable of
     *         converting from String to target type has been registered
     */
    @Nullable
    public <T> ResourceConverter<String, T> converterFor(@NotNull Class<T> targetType)
    {
        assertNotNull(targetType, Class.class, "targetType");
        return (ResourceConverter<String, T>) converterFor(String.class, targetType);
    }


    /**
     * @param sourceType the Class of the source value to be converted
     * @param targetType the desired Class type of the converted value
     * @return a converter that will convert an object value of Class sourceType to the target type. Returns null if no converter cabable of
     *         converting from sourceType to target type has been registered
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public <S, D> ResourceConverter<S, D> converterFor(@NotNull Class<S> sourceType, @NotNull Class<D> targetType)
    {
        assertNotNull(sourceType, Class.class, "sourceType");
        assertNotNull(targetType, Class.class, "targetType");
        if (targetType.isPrimitive())
        {

            targetType = wrapperClassForPrimativeType(targetType); //not sure about this cast. Can this ever fail?
        }

        Map<Class<?>, ResourceConverter<?,?>> destMap = registry.get(sourceType);
        if (destMap == null)
        {

            return null;
        }
        //if cast above worked, this here cannot fail, because destMap returns only those converters of source type Class<S>, and the following line
        //ensures the dest type of the converter is Class<D>, because targetType *is* a Clas<D>
        @SuppressWarnings("unchecked")
        ResourceConverter<S, D> castResult = (ResourceConverter<S, D>) destMap.get(targetType);
        return castResult;

    }

    //convert a Class of a primitive type to its Object-derived wrapper type.
    //return the argument if the argument is not a primitive type
    private Class wrapperClassForPrimativeType(Class primitiveClassType)
    {
        Class converted = primitiveClassType;
        if (primitiveClassType == Boolean.TYPE)
        {
            converted = Boolean.class;
        }
        else if (primitiveClassType == Character.TYPE)
        {
            converted = Character.class;
        }
        else if (primitiveClassType == Byte.TYPE)
        {
            converted = Byte.class;
        }
        else if (primitiveClassType == Short.TYPE)
        {
            converted = Short.class;
        }
        else if (primitiveClassType == Integer.TYPE)
        {
            converted = Integer.class;
        }
        else if (primitiveClassType == Long.TYPE)
        {
            converted = Long.class;
        }
        else if (primitiveClassType == Float.TYPE)
        {
            converted = Float.class;
        }
        else if (primitiveClassType == Double.TYPE)
        {
            converted = Double.class;
        }
        return converted;
    }



    /**
     * Returns a new instance of a List of all ResourceConverters registered to convert from the source Class type in
     * the argument.
     *
     * @param sourceType the source type of the converters of interest. For example, java.lang.String returns all registered
     *                   converters that know how to convert a String into something else.
     * @return a new List of all ResourceConverters registered as being able to convert the source type in the argument.
     *         The actual ResourceConverter instances are not copied, i.e., they are the same instances as those the class manages.
     *         Of these, only StringToBoolean has mutable state (trueValues), and you are allowed to modify that state if desired.
     */
    @NotNull
    public List<ResourceConverter> convertersFor(@NotNull Class<?> sourceType)
    {
        assertNotNull(sourceType, Class.class, "sourceType");
        Map<Class<?>, ResourceConverter<?, ?>> destMap = registry.get(sourceType);
        List<ResourceConverter> list = new ArrayList<ResourceConverter>();
        if (destMap != null)
        {
            list.addAll(destMap.values());
        }
        return list;
    }

    /**
     * @return all the converters registered in this instance
     */
    @NotNull
    public List<ResourceConverter> allConverters()
    {
        List<ResourceConverter> list = new ArrayList<ResourceConverter>();
        for (Class<?> sourceKey : registry.keySet())
        {
            Map<Class<?>, ResourceConverter<?,?>> destMap = registry.get(sourceKey);
            if (destMap != null)
            {
                list.addAll(destMap.values());
            }
        }
        return list;
    }


    /**
     * This will populate this registry will all ResourceConverters in the convert sub-package. After calling this method,
     * you are still free to add new converters, and subsitute your own custom converters for any source/dest Class type.
     */
    public void addDefaultConverters()
    {
        //Developer note : if you add a new converter to the default list, make sure to add the converted type to
        //IMMUTABLE_TYPES Set if it is an immutable class
        ResourceConverter[] defaults = new ResourceConverter[]{
                new StringToBoolean(),
                new StringToCharacter(),
                new StringToColor(),
                new StringToDimension(),
                new StringToEmptyBorder(),
                new StringToFont(),
                new StringToImage(),
                new StringToImage.StringToBufferedImage(),
                new StringToImage.StringToIcon(),
                new StringToImage.StringToImageIcon(),
                new StringToInsets(),
                new StringToKeyStroke(),
                new StringToNumber.StringToByte(),
                new StringToNumber.StringToDouble(),
                new StringToNumber.StringToFloat(),
                new StringToNumber.StringToInteger(),
                new StringToNumber.StringToLong(),
                new StringToNumber.StringToShort(),
                new StringToPoint(),
                new StringToPoint.StringToPoint2D_Double(),
                new StringToPoint.StringToPoint2D_Float(),
                new StringToRectangle(),
                new StringToRectangle.StringToRectangle2D_Double(),
                new StringToRectangle.StringToRectangle2D_Float(),
                new StringToURI(),
                new StringToURL(),
                new StringToMnemonicTextValue(),
        };
        addAll(Arrays.asList(defaults));
    }

    //This map treats the SOURCE type as the Key. The value of the outer map is itself a map, which maps the DESTINATION Class to an actual ResourceConveter
    private final Map<Class<?>, Map<Class<?>, ResourceConverter<?, ?>>> registry = new ConcurrentHashMap<Class<?>, Map<Class<?>, ResourceConverter<?,?>>>(32, .75f, 4);


    private final static Set<Class<?>> IMMUTABLE_TYPES = new HashSet<Class<?>>(32);
    static{
        //these known conversion types are immutable
        IMMUTABLE_TYPES.add(String.class);
        IMMUTABLE_TYPES.add(Boolean.class);
        IMMUTABLE_TYPES.add(Character.class);
        IMMUTABLE_TYPES.add(Color.class);
        IMMUTABLE_TYPES.add(Font.class);
        IMMUTABLE_TYPES.add(BufferedImage.class); //Not technically immutable, but these image classes
        IMMUTABLE_TYPES.add(Image.class);         //are heavyweight objects and we don't want to clone them
        IMMUTABLE_TYPES.add(Icon.class);
        IMMUTABLE_TYPES.add(ImageIcon.class);
        IMMUTABLE_TYPES.add(KeyStroke.class);
        IMMUTABLE_TYPES.add(Byte.class);
        IMMUTABLE_TYPES.add(Short.class);
        IMMUTABLE_TYPES.add(Integer.class);
        IMMUTABLE_TYPES.add(Long.class);
        IMMUTABLE_TYPES.add(Float.class);
        IMMUTABLE_TYPES.add(Double.class);
        IMMUTABLE_TYPES.add(URI.class);
        IMMUTABLE_TYPES.add(URL.class);
        IMMUTABLE_TYPES.add(MnemonicTextValue.class);
    }

    /**
     *
     * @return the set of Classes for which there are known ResourceConverters and which are immutable. This information is
     * used when ResourceMap returns a converted value. It will try to make a copy of any mutable types, i.e., types not
     * in the returned Set
     */
    public static Set<Class<?>> getImmutableTypes()
    {
        return IMMUTABLE_TYPES;
    }

    /**
     * Adds the class in the argument to the internal list of immutable classes.
     * @param type a Class that is to be treated as immutable. This information is useful when deciding whether or not
     * to make a copy of it when getting such a type from a ResourceMap
     */
    public synchronized static void addImmutableType(Class<?> type)
    {
        if (type != null)
        {
            IMMUTABLE_TYPES.add(type);
        }
    }

    /**
     *
     * @param type the Class to remove from the Set of immutable types.
     * @return true if the type was presenent in the set of immutable types, otherwise false. Either way, when this method
     * returns the type will not be in the immutable type set.
     */
    public synchronized static boolean removeImmutableType(Class<?> type)
    {
        if (type != null)
        {
            return IMMUTABLE_TYPES.remove(type);
        }
        return false;
    }

    protected void assertNotNull(Object o, Class type, String paramName)
    {
        if (o == null)
        {
            throw new IllegalArgumentException(String.format("parameter '%s' of type '%s' cannot be null.", paramName, type));
        }
    }
}