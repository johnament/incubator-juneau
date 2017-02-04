// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau;

import static org.apache.juneau.ClassMeta.ClassCategory.*;
import static org.apache.juneau.internal.ClassUtils.*;

import java.io.*;
import java.lang.reflect.*;
import java.lang.reflect.Proxy;
import java.net.*;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;
import org.apache.juneau.utils.*;

/**
 * A wrapper class around the {@link Class} object that provides cached information
 * about that class.
 *
 * <p>
 * 	Instances of this class can be created through the {@link BeanContext#getClassMeta(Class)} method.
 * <p>
 * 	The {@link BeanContext} class will cache and reuse instances of this class except for the following class types:
 * <ul>
 * 	<li>Arrays
 * 	<li>Maps with non-Object key/values.
 * 	<li>Collections with non-Object key/values.
 * </ul>
 * <p>
 * 	This class is tied to the {@link BeanContext} class because it's that class that makes the determination
 * 	of what is a bean.
 *
 * @param <T> The class type of the wrapped class.
 */
@Bean(properties="innerClass,classCategory,elementType,keyType,valueType,notABeanReason,initException,beanMeta")
public final class ClassMeta<T> implements Type {

	/** Class categories. */
	enum ClassCategory {
		MAP, COLLECTION, CLASS, NUMBER, DECIMAL, BOOLEAN, CHAR, DATE, ARRAY, ENUM, OTHER, CHARSEQ, STR, OBJ, URI, BEANMAP, READER, INPUTSTREAM
	}

	final Class<T> innerClass;                              // The class being wrapped.
	final Class<? extends T> implClass;                     // The implementation class to use if this is an interface.
	final ClassCategory cc;                                 // The class category.
	final Method fromStringMethod;                          // The static valueOf(String) or fromString(String) or forString(String) method (if it has one).
	final Constructor<? extends T>
		noArgConstructor;                                    // The no-arg constructor for this class (if it has one).
	final Constructor<T>
		stringConstructor,                                   // The X(String) constructor (if it has one).
		numberConstructor,                                   // The X(Number) constructor (if it has one).
		swapConstructor,                                     // The X(Swappable) constructor (if it has one).
		objectMapConstructor;                                // The X(ObjectMap) constructor (if it has one).
	final Class<?>
		swapMethodType,                                      // The class type of the object in the number constructor.
		numberConstructorType;
	final Method
		toObjectMapMethod,                                   // The toObjectMap() method (if it has one).
		swapMethod,                                          // The swap() method (if it has one).
		namePropertyMethod,                                  // The method to set the name on an object (if it has one).
		parentPropertyMethod;                                // The method to set the parent on an object (if it has one).
	final boolean
		isDelegate,                                          // True if this class extends Delegate.
		isAbstract,                                          // True if this class is abstract.
		isMemberClass;                                       // True if this is a non-static member class.
	final Object primitiveDefault;                          // Default value for primitive type classes.
	final Map<String,Method>
		remoteableMethods,                                   // Methods annotated with @Remoteable.  Contains all public methods if class is annotated with @Remotable.
		publicMethods;                                       // All public methods, including static methods.
	final PojoSwap<?,?>[] childPojoSwaps;                   // Any PojoSwaps where the normal type is a subclass of this class.
	final ConcurrentHashMap<Class<?>,PojoSwap<?,?>>
		childSwapMap,                                        // Maps normal subclasses to PojoSwaps.
		childUnswapMap;                                      // Maps swap subclasses to PojoSwaps.

	final BeanContext beanContext;                    // The bean context that created this object.
	ClassMeta<?>
		serializedClassMeta,                           // The transformed class type (if class has swap associated with it).
		elementType = null,                            // If ARRAY or COLLECTION, the element class type.
		keyType = null,                                // If MAP, the key class type.
		valueType = null;                              // If MAP, the value class type.
	InvocationHandler invocationHandler;              // The invocation handler for this class (if it has one).
	BeanMeta<T> beanMeta;                             // The bean meta for this bean class (if it's a bean).
	String dictionaryName, resolvedDictionaryName;    // The dictionary name of this class if it has one.
	String notABeanReason;                            // If this isn't a bean, the reason why.
	PojoSwap<T,?> pojoSwap;                           // The object POJO swap associated with this bean (if it has one).
	BeanFilter beanFilter;                            // The bean filter associated with this bean (if it has one).

	private MetadataMap extMeta = new MetadataMap();  // Extended metadata
	private Throwable initException;                  // Any exceptions thrown in the init() method.

	private static final Boolean BOOLEAN_DEFAULT = false;
	private static final Character CHARACTER_DEFAULT = (char)0;
	private static final Short SHORT_DEFAULT = (short)0;
	private static final Integer INTEGER_DEFAULT = 0;
	private static final Long LONG_DEFAULT = 0l;
	private static final Float FLOAT_DEFAULT = 0f;
	private static final Double DOUBLE_DEFAULT = 0d;
	private static final Byte BYTE_DEFAULT = (byte)0;

	/**
	 * Shortcut for calling <code>ClassMeta(innerClass, beanContext, <jk>false</jk>)</code>.
	 */
	ClassMeta(Class<T> innerClass, BeanContext beanContext, Class<? extends T> implClass, BeanFilter beanFilter, PojoSwap<T,?> pojoSwap, PojoSwap<?,?>[] childPojoSwaps) {
		this(innerClass, beanContext, implClass, beanFilter, pojoSwap, childPojoSwaps, false);
	}

	/**
	 * Construct a new {@code ClassMeta} based on the specified {@link Class}.
	 *
	 * @param innerClass The class being wrapped.
	 * @param beanContext The bean context that created this object.
	 * @param implClass For interfaces and abstract classes, this represents the "real" class to instantiate.
	 * 	Can be <jk>null</jk>.
	 * @param beanFilter The {@link BeanFilter} programmatically associated with this class.
	 * 	Can be <jk>null</jk>.
	 * @param pojoSwap The {@link PojoSwap} programmatically associated with this class.
	 * 	Can be <jk>null</jk>.
	 * @param childPojoSwap The child {@link PojoSwap PojoSwaps} programmatically associated with this class.
	 * 	These are the <code>PojoSwaps</code> that have normal classes that are subclasses of this class.
	 * 	Can be <jk>null</jk>.
	 * @param delayedInit Don't call init() in constructor.
	 * 	Used for delayed initialization when the possibility of class reference loops exist.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	ClassMeta(Class<T> innerClass, BeanContext beanContext, Class<? extends T> implClass, BeanFilter beanFilter, PojoSwap<T,?> pojoSwap, PojoSwap<?,?>[] childPojoSwaps, boolean delayedInit) {
		this.innerClass = innerClass;
		this.beanContext = beanContext;
		this.implClass = implClass;
		this.childPojoSwaps = childPojoSwaps;
		this.childSwapMap = childPojoSwaps == null ? null : new ConcurrentHashMap<Class<?>,PojoSwap<?,?>>();
		this.childUnswapMap = childPojoSwaps == null ? null : new ConcurrentHashMap<Class<?>,PojoSwap<?,?>>();

		Class<T> c = innerClass;
		ClassCategory _cc = ClassCategory.OTHER;
		boolean _isDelegate = false;
		Method
			_fromStringMethod = null,
			_toObjectMapMethod = null,
			_swapMethod = null,
			_parentPropertyMethod = null,
			_namePropertyMethod = null;
		Constructor<T>
			_noArgConstructor = null,
			_stringConstructor = null,
			_objectMapConstructor = null,
			_swapConstructor = null,
			_numberConstructor = null;
		Class<?>
			_swapMethodType = null,
			_numberConstructorType = null;
		Object _primitiveDefault = null;
		Map<String,Method>
			_publicMethods = new LinkedHashMap<String,Method>(),
			_remoteableMethods = null;

		if (c.isPrimitive()) {
			if (c == Boolean.TYPE)
				_cc = BOOLEAN;
			else if (c == Byte.TYPE || c == Short.TYPE || c == Integer.TYPE || c == Long.TYPE || c == Float.TYPE || c == Double.TYPE) {
				if (c == Float.TYPE || c == Double.TYPE)
					_cc = DECIMAL;
				else
					_cc = NUMBER;
			}
			else if (c == Character.TYPE)
				_cc = CHAR;
		} else {
			if (isParentClass(Delegate.class, c))
				_isDelegate = true;

			if (c == Object.class)
				_cc = OBJ;
			else if (c.isEnum())
				_cc = ENUM;
			else if (c.equals(Class.class))
				_cc = CLASS;
			else if (isParentClass(CharSequence.class, c)) {
				if (c.equals(String.class))
					_cc = STR;
				else
					_cc = CHARSEQ;
			}
			else if (isParentClass(Number.class, c)) {
				if (isParentClass(Float.class, c) || isParentClass(Double.class, c))
					_cc = DECIMAL;
				else
					_cc = NUMBER;
			}
			else if (isParentClass(Collection.class, c))
				_cc = COLLECTION;
			else if (isParentClass(Map.class, c)) {
				if (isParentClass(BeanMap.class, c))
					_cc = BEANMAP;
				else
					_cc = MAP;
			}
			else if (c == Character.class)
				_cc = CHAR;
			else if (c == Boolean.class)
				_cc = BOOLEAN;
			else if (isParentClass(Date.class, c) || isParentClass(Calendar.class, c))
				_cc = DATE;
			else if (c.isArray())
				_cc = ARRAY;
			else if (isParentClass(URL.class, c) || isParentClass(URI.class, c) || c.isAnnotationPresent(org.apache.juneau.annotation.URI.class))
				_cc = URI;
			else if (isParentClass(Reader.class, c))
				_cc = READER;
			else if (isParentClass(InputStream.class, c))
				_cc = INPUTSTREAM;
		}

		isMemberClass = c.isMemberClass() && ! isStatic(c);

		// Find static fromString(String) or equivalent method.
		// fromString() must be checked before valueOf() so that Enum classes can create their own
		//		specialized fromString() methods to override the behavior of Enum.valueOf(String).
		// valueOf() is used by enums.
		// parse() is used by the java logging Level class.
		// forName() is used by Class and Charset
		for (String methodName : new String[]{"fromString","valueOf","parse","parseString","forName","forString"}) {
			if (_fromStringMethod == null) {
				for (Method m : c.getMethods()) {
					if (isStatic(m) && isPublic(m) && isNotDeprecated(m)) {
						String mName = m.getName();
						if (mName.equals(methodName) && m.getReturnType() == c) {
							Class<?>[] args = m.getParameterTypes();
							if (args.length == 1 && args[0] == String.class) {
								_fromStringMethod = m;
								break;
							}
						}
					}
				}
			}
		}

		// Special cases
		try {
			if (c == TimeZone.class)
				_fromStringMethod = c.getMethod("getTimeZone", String.class);
			else if (c == Locale.class)
				_fromStringMethod = LocaleAsString.class.getMethod("fromString", String.class);
		} catch (NoSuchMethodException e1) {}

		// Find toObjectMap() method if present.
		for (Method m : c.getMethods()) {
			if (isPublic(m) && isNotDeprecated(m) && ! isStatic(m)) {
				String mName = m.getName();
				if (mName.equals("toObjectMap")) {
					if (m.getParameterTypes().length == 0 && m.getReturnType() == ObjectMap.class) {
						_toObjectMapMethod = m;
						break;
					}
				} else if (mName.equals("swap")) {
					if (m.getParameterTypes().length == 1 && m.getParameterTypes()[0] == BeanSession.class) {
						_swapMethod = m;
						_swapMethodType = m.getReturnType();
						break;
					}
				}
			}
		}

		// Find @NameProperty and @ParentProperty methods if present.
		for (Method m : c.getDeclaredMethods()) {
			if (m.isAnnotationPresent(ParentProperty.class) && m.getParameterTypes().length == 1) {
				m.setAccessible(true);
				_parentPropertyMethod = m;
			}
			if (m.isAnnotationPresent(NameProperty.class) && m.getParameterTypes().length == 1) {
				m.setAccessible(true);
				_namePropertyMethod = m;
			}
		}

		// Note:  Primitive types are normally abstract.
		isAbstract = Modifier.isAbstract(c.getModifiers()) && ! c.isPrimitive();

		// Find constructor(String) method if present.
		for (Constructor cs : c.getConstructors()) {
			if (isPublic(cs) && isNotDeprecated(cs)) {
				Class<?>[] args = cs.getParameterTypes();
				if (args.length == (isMemberClass ? 1 : 0) && c != Object.class && ! isAbstract) {
					_noArgConstructor = cs;
				} else if (args.length == (isMemberClass ? 2 : 1)) {
					Class<?> arg = args[(isMemberClass ? 1 : 0)];
					if (arg == String.class)
						_stringConstructor = cs;
					else if (ObjectMap.class.isAssignableFrom(arg))
						_objectMapConstructor = cs;
					else if (_swapMethodType != null && _swapMethodType.isAssignableFrom(arg))
						_swapConstructor = cs;
					else if (_cc != NUMBER && (Number.class.isAssignableFrom(arg) || (arg.isPrimitive() && (arg == int.class || arg == short.class || arg == long.class || arg == float.class || arg == double.class)))) {
						_numberConstructor = cs;
						_numberConstructorType = ClassUtils.getWrapperIfPrimitive(arg);
					}
				}
			}
		}

		if (c.isPrimitive()) {
			if (c == Boolean.TYPE)
				_primitiveDefault = BOOLEAN_DEFAULT;
			else if (c == Character.TYPE)
				_primitiveDefault = CHARACTER_DEFAULT;
			else if (c == Short.TYPE)
				_primitiveDefault = SHORT_DEFAULT;
			else if (c == Integer.TYPE)
				_primitiveDefault = INTEGER_DEFAULT;
			else if (c == Long.TYPE)
				_primitiveDefault = LONG_DEFAULT;
			else if (c == Float.TYPE)
				_primitiveDefault = FLOAT_DEFAULT;
			else if (c == Double.TYPE)
				_primitiveDefault = DOUBLE_DEFAULT;
			else if (c == Byte.TYPE)
				_primitiveDefault = BYTE_DEFAULT;
		} else {
			if (c == Boolean.class)
				_primitiveDefault = BOOLEAN_DEFAULT;
			else if (c == Character.class)
				_primitiveDefault = CHARACTER_DEFAULT;
			else if (c == Short.class)
				_primitiveDefault = SHORT_DEFAULT;
			else if (c == Integer.class)
				_primitiveDefault = INTEGER_DEFAULT;
			else if (c == Long.class)
				_primitiveDefault = LONG_DEFAULT;
			else if (c == Float.class)
				_primitiveDefault = FLOAT_DEFAULT;
			else if (c == Double.class)
				_primitiveDefault = DOUBLE_DEFAULT;
			else if (c == Byte.class)
				_primitiveDefault = BYTE_DEFAULT;
		}

		for (Method m : c.getMethods())
			if (isPublic(m) && isNotDeprecated(m))
				_publicMethods.put(ClassUtils.getMethodSignature(m), m);

		if (c.getAnnotation(Remoteable.class) != null) {
			_remoteableMethods = _publicMethods;
		} else {
			for (Method m : c.getMethods()) {
				if (m.getAnnotation(Remoteable.class) != null) {
					if (_remoteableMethods == null)
						_remoteableMethods = new LinkedHashMap<String,Method>();
					_remoteableMethods.put(ClassUtils.getMethodSignature(m), m);
				}
			}
		}

		if (innerClass != Object.class) {
			_noArgConstructor = (Constructor<T>)findNoArgConstructor(implClass == null ? innerClass : implClass, Visibility.PUBLIC);
		}

		if (beanFilter == null)
			beanFilter = findBeanFilter();

		PojoSwap ps = null;
		if (_swapMethod != null) {
			ps = new PojoSwap<T,Object>(c, _swapMethod.getReturnType()) {
				@Override
				public Object swap(BeanSession session, Object o) throws SerializeException {
					try {
						return swapMethod.invoke(o, session);
					} catch (Exception e) {
						throw new SerializeException(e);
					}
				}
				@Override
				public T unswap(BeanSession session, Object f, ClassMeta<?> hint) throws ParseException {
					try {
						if (swapConstructor != null)
							return swapConstructor.newInstance(f);
						return super.unswap(session, f, hint);
					} catch (Exception e) {
						throw new ParseException(e);
					}
				}
			};
		}
		if (ps == null)
			ps = findPojoSwap();
		if (ps == null)
			ps = pojoSwap;

		this.cc = _cc;
		this.isDelegate = _isDelegate;
		this.fromStringMethod = _fromStringMethod;
		this.toObjectMapMethod = _toObjectMapMethod;
		this.swapMethod = _swapMethod;
		this.swapMethodType = _swapMethodType;
		this.parentPropertyMethod = _parentPropertyMethod;
		this.namePropertyMethod =_namePropertyMethod;
		this.noArgConstructor = _noArgConstructor;
		this.stringConstructor = _stringConstructor;
		this.objectMapConstructor =_objectMapConstructor;
		this.swapConstructor = _swapConstructor;
		this.numberConstructor = _numberConstructor;
		this.numberConstructorType = _numberConstructorType;
		this.primitiveDefault = _primitiveDefault;
		this.publicMethods = _publicMethods;
		this.remoteableMethods = _remoteableMethods;
		this.beanFilter = beanFilter;
		this.pojoSwap = ps;

		if (! delayedInit)
			init();
	}

	/**
	 * Copy constructor.
	 * Used for creating Map and Collection class metas that shouldn't be cached.
	 */
	ClassMeta(ClassMeta<T> mainType, ClassMeta<?> keyType, ClassMeta<?> valueType, ClassMeta<?> elementType) {
		this.innerClass = mainType.innerClass;
		this.implClass = mainType.implClass;
		this.childPojoSwaps = mainType.childPojoSwaps;
		this.childSwapMap = mainType.childSwapMap;
		this.childUnswapMap = mainType.childUnswapMap;
		this.cc = mainType.cc;
		this.fromStringMethod = mainType.fromStringMethod;
		this.noArgConstructor = mainType.noArgConstructor;
		this.stringConstructor = mainType.stringConstructor;
		this.numberConstructor = mainType.numberConstructor;
		this.swapConstructor = mainType.swapConstructor;
		this.objectMapConstructor = mainType.objectMapConstructor;
		this.swapMethodType = mainType.swapMethodType;
		this.numberConstructorType = mainType.numberConstructorType;
		this.toObjectMapMethod = mainType.toObjectMapMethod;
		this.swapMethod = mainType.swapMethod;
		this.namePropertyMethod = mainType.namePropertyMethod;
		this.parentPropertyMethod = mainType.parentPropertyMethod;
		this.isDelegate = mainType.isDelegate;
		this.isAbstract = mainType.isAbstract;
		this.isMemberClass = mainType.isMemberClass;
		this.primitiveDefault = mainType.primitiveDefault;
		this.remoteableMethods = mainType.remoteableMethods;
		this.publicMethods = mainType.publicMethods;
		this.beanContext = mainType.beanContext;
		this.serializedClassMeta = this;
		this.elementType = elementType;
		this.keyType = keyType;
		this.valueType = valueType;
		this.invocationHandler = mainType.invocationHandler;
		this.beanMeta = mainType.beanMeta;
		this.dictionaryName = mainType.dictionaryName;
		this.resolvedDictionaryName = mainType.resolvedDictionaryName;
		this.notABeanReason = mainType.notABeanReason;
		this.pojoSwap = mainType.pojoSwap;
		this.beanFilter = mainType.beanFilter;
		this.extMeta = mainType.extMeta;
		this.initException = mainType.initException;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	ClassMeta init() {

		try {

			Class c = innerClass;

			if (swapMethod != null) {
				this.pojoSwap = new PojoSwap<T,Object>(c, swapMethod.getReturnType()) {
					@Override
					public Object swap(BeanSession session, Object o) throws SerializeException {
						try {
							return swapMethod.invoke(o, session);
						} catch (Exception e) {
							throw new SerializeException(e);
						}
					}
					@Override
					public T unswap(BeanSession session, Object f, ClassMeta<?> hint) throws ParseException {
						try {
							if (swapConstructor != null)
								return swapConstructor.newInstance(f);
							return super.unswap(session, f, hint);
						} catch (Exception e) {
							throw new ParseException(e);
						}
					}
				};
			}

			serializedClassMeta = (pojoSwap == null ? this : findClassMeta(pojoSwap.getSwapClass()));
			if (serializedClassMeta == null)
				serializedClassMeta = this;

			// If this is an array, get the element type.
			if (cc == ARRAY)
				elementType = findClassMeta(innerClass.getComponentType());

			// If this is a MAP, see if it's parameterized (e.g. AddressBook extends HashMap<String,Person>)
			else if (cc == MAP) {
				ClassMeta[] parameters = findParameters();
				if (parameters != null && parameters.length == 2) {
					keyType = parameters[0];
					valueType = parameters[1];
				} else {
					keyType = findClassMeta(Object.class);
					valueType = findClassMeta(Object.class);
				}
			}

			// If this is a COLLECTION, see if it's parameterized (e.g. AddressBook extends LinkedList<Person>)
			else if (cc == COLLECTION) {
				ClassMeta[] parameters = findParameters();
				if (parameters != null && parameters.length == 1) {
					elementType = parameters[0];
				} else {
					elementType = findClassMeta(Object.class);
				}
			}

			// If the category is unknown, see if it's a bean.
			// Note that this needs to be done after all other initialization has been done.
			else if (cc == OTHER) {

				BeanMeta newMeta = null;
				try {
					newMeta = new BeanMeta(this, beanContext, beanFilter, null);
					notABeanReason = newMeta.notABeanReason;
				} catch (RuntimeException e) {
					notABeanReason = e.getMessage();
					throw e;
				}
				if (notABeanReason == null)
					beanMeta = newMeta;
			}

		} catch (NoClassDefFoundError e) {
			this.initException = e;
		} catch (RuntimeException e) {
			this.initException = e;
			throw e;
		}

		if (isBean())
			dictionaryName = resolvedDictionaryName = getBeanMeta().getDictionaryName();

		if (isArray()) {
			resolvedDictionaryName = getElementType().getResolvedDictionaryName();
			if (resolvedDictionaryName != null)
				resolvedDictionaryName += "^";
		}

		return this;
	}

	private ClassMeta<?> findClassMeta(Class<?> c) {
		return beanContext.getClassMeta(c);
	}

	private ClassMeta<?>[] findParameters() {
		return beanContext.findParameters(innerClass, innerClass);
	}

	private BeanFilter findBeanFilter() {
		try {
			List<Bean> ba = ReflectionUtils.findAnnotations(Bean.class, innerClass);
			if (! ba.isEmpty())
				return new AnnotationBeanFilterBuilder(innerClass, ba).build();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private PojoSwap<T,?> findPojoSwap() {
		try {
			Pojo p = innerClass.getAnnotation(Pojo.class);
			if (p != null) {
				Class<?> c = p.swap();
				if (c != Null.class) {
					if (ClassUtils.isParentClass(PojoSwap.class, c))
						return (PojoSwap<T,?>)c.newInstance();
					throw new RuntimeException("TODO - Surrogate classes not yet supported.");
				}
			}
			return null;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns the bean dictionary name associated with this class.
	 * <p>
	 * The lexical name is defined by {@link Bean#typeName()}.
	 *
	 * @return The type name associated with this bean class, or <jk>null</jk> if there is no type name defined or this isn't a bean.
	 */
	public String getDictionaryName() {
		return dictionaryName;
	}

	/**
	 * Returns the resolved bean dictionary name associated with this class.
	 * <p>
	 * Unlike {@link #getDictionaryName()}, this method automatically resolves multidimensional arrays
	 *  (e.g. <js>"X^^"</js> and returns array class metas accordingly if the base class has a type name.
	 *
	 * @return The type name associated with this bean class, or <jk>null</jk> if there is no type name defined or this isn't a bean.
	 */
	public String getResolvedDictionaryName() {
		return resolvedDictionaryName;
	}

	/**
	 * Returns the category of this class.
	 *
	 * @return The category of this class.
	 */
	public ClassCategory getClassCategory() {
		return cc;
	}

	/**
	 * Returns <jk>true</jk> if this class is a superclass of or the same as the specified class.
	 *
	 * @param c The comparison class.
	 * @return <jk>true</jk> if this class is a superclass of or the same as the specified class.
	 */
	public boolean isAssignableFrom(Class<?> c) {
		return isParentClass(innerClass, c);
	}

	/**
	 * Returns <jk>true</jk> if this class as subtypes defined through {@link Bean#subTypes}.
	 *
	 * @return <jk>true</jk> if this class has subtypes.
	 */
	public boolean hasSubTypes() {
		return beanFilter != null && beanFilter.getSubTypeProperty() != null;
	}

	/**
	 * Returns <jk>true</jk> if this class is a subclass of or the same as the specified class.
	 *
	 * @param c The comparison class.
	 * @return <jk>true</jk> if this class is a subclass of or the same as the specified class.
	 */
	public boolean isInstanceOf(Class<?> c) {
		return isParentClass(c, innerClass);
	}

	/**
	 * Returns <jk>true</jk> if this class or any child classes has a {@link PojoSwap} associated with it.
	 * <p>
	 * Used when transforming bean properties to prevent having to look up transforms if we know for certain
	 * that no transforms are associated with a bean property.
	 *
	 * @return <jk>true</jk> if this class or any child classes has a {@link PojoSwap} associated with it.
	 */
	protected boolean hasChildPojoSwaps() {
		return childPojoSwaps != null;
	}

	/**
	 * Returns the {@link PojoSwap} where the specified class is the same/subclass of the normal class of
	 * one of the child pojo swaps associated with this class.
	 *
	 * @param normalClass The normal class being resolved.
	 * @return The resolved {@link PojoSwap} or <jk>null</jk> if none were found.
	 */
	protected PojoSwap<?,?> getChildPojoSwapForSwap(Class<?> normalClass) {
		if (childSwapMap != null) {
			PojoSwap<?,?> s = childSwapMap.get(normalClass);
			if (s == null) {
				for (PojoSwap<?,?> f : childPojoSwaps)
					if (s == null && isParentClass(f.getNormalClass(), normalClass))
						s = f;
				if (s == null)
					 s = PojoSwap.NULL;
				childSwapMap.putIfAbsent(normalClass, s);
			}
			if (s == PojoSwap.NULL)
				return null;
			return s;
		}
		return null;
	}

	/**
	 * Returns the {@link PojoSwap} where the specified class is the same/subclass of the swap class of
	 * one of the child pojo swaps associated with this class.
	 *
	 * @param swapClass The swap class being resolved.
	 * @return The resolved {@link PojoSwap} or <jk>null</jk> if none were found.
	 */
	protected PojoSwap<?,?> getChildPojoSwapForUnswap(Class<?> swapClass) {
		if (childUnswapMap != null) {
			PojoSwap<?,?> s = childUnswapMap.get(swapClass);
			if (s == null) {
				for (PojoSwap<?,?> f : childPojoSwaps)
					if (s == null && isParentClass(f.getSwapClass(), swapClass))
						s = f;
				if (s == null)
					 s = PojoSwap.NULL;
				childUnswapMap.putIfAbsent(swapClass, s);
			}
			if (s == PojoSwap.NULL)
				return null;
			return s;
		}
		return null;
	}

	/**
	 * Locates the no-arg constructor for the specified class.
	 * Constructor must match the visibility requirements specified by parameter 'v'.
	 * If class is abstract, always returns <jk>null</jk>.
	 * Note that this also returns the 1-arg constructor for non-static member classes.
	 *
	 * @param c The class from which to locate the no-arg constructor.
	 * @param v The minimum visibility.
	 * @return The constructor, or <jk>null</jk> if no no-arg constructor exists with the required visibility.
	 */
	@SuppressWarnings({"rawtypes","unchecked"})
	protected static <T> Constructor<? extends T> findNoArgConstructor(Class<T> c, Visibility v) {
		int mod = c.getModifiers();
		if (Modifier.isAbstract(mod))
			return null;
		boolean isMemberClass = c.isMemberClass() && ! isStatic(c);
		for (Constructor cc : c.getConstructors()) {
			mod = cc.getModifiers();
			if (cc.getParameterTypes().length == (isMemberClass ? 1 : 0) && v.isVisible(mod) && isNotDeprecated(cc))
				return v.transform(cc);
		}
		return null;
	}

	/**
	 * Set element type on non-cached <code>Collection</code> types.
	 *
	 * @param elementType The class type for elements in the collection class represented by this metadata.
	 * @return This object (for method chaining).
	 */
	protected ClassMeta<T> setElementType(ClassMeta<?> elementType) {
		this.elementType = elementType;
		return this;
	}

	/**
	 * Set key type on non-cached <code>Map</code> types.
	 *
	 * @param keyType The class type for keys in the map class represented by this metadata.
	 * @return This object (for method chaining).
	 */
	protected ClassMeta<T> setKeyType(ClassMeta<?> keyType) {
		this.keyType = keyType;
		return this;
	}

	/**
	 * Set value type on non-cached <code>Map</code> types.
	 *
	 * @param valueType The class type for values in the map class represented by this metadata.
	 * @return This object (for method chaining).
	 */
	protected ClassMeta<T> setValueType(ClassMeta<?> valueType) {
		this.valueType = valueType;
		return this;
	}

	/**
	 * Returns the {@link Class} object that this class type wraps.
	 *
	 * @return The wrapped class object.
	 */
	public Class<T> getInnerClass() {
		return innerClass;
	}

	/**
	 * Returns the serialized (swapped) form of this class if there is an {@link PojoSwap} associated with it.
	 *
	 * @return The serialized class type, or this object if no swap is associated with the class.
	 */
	@BeanIgnore
	public ClassMeta<?> getSerializedClassMeta() {
		return serializedClassMeta;
	}

	/**
	 * For array and {@code Collection} types, returns the class type of the components of the array or {@code Collection}.
	 *
	 * @return The element class type, or <jk>null</jk> if this class is not an array or Collection.
	 */
	public ClassMeta<?> getElementType() {
		return elementType;
	}

	/**
	 * For {@code Map} types, returns the class type of the keys of the {@code Map}.
	 *
	 * @return The key class type, or <jk>null</jk> if this class is not a Map.
	 */
	public ClassMeta<?> getKeyType() {
		return keyType;
	}

	/**
	 * For {@code Map} types, returns the class type of the values of the {@code Map}.
	 *
	 * @return The value class type, or <jk>null</jk> if this class is not a Map.
	 */
	public ClassMeta<?> getValueType() {
		return valueType;
	}

	/**
	 * Returns <jk>true</jk> if this class implements {@link Delegate}, meaning
	 * 	it's a representation of some other object.
	 *
	 * @return <jk>true</jk> if this class implements {@link Delegate}.
	 */
	public boolean isDelegate() {
		return isDelegate;
	}

	/**
	 * Returns <jk>true</jk> if this class is a subclass of {@link Map}.
	 *
	 * @return <jk>true</jk> if this class is a subclass of {@link Map}.
	 */
	public boolean isMap() {
		return cc == MAP || cc == BEANMAP;
	}

	/**
	 * Returns <jk>true</jk> if this class is a subclass of {@link Map} or it's a bean.
	 *
	 * @return <jk>true</jk> if this class is a subclass of {@link Map} or it's a bean.
	 */
	public boolean isMapOrBean() {
		return cc == MAP || cc == BEANMAP || beanMeta != null;
	}

	/**
	 * Returns <jk>true</jk> if this class is a subclass of {@link BeanMap}.
	 *
	 * @return <jk>true</jk> if this class is a subclass of {@link BeanMap}.
	 */
	public boolean isBeanMap() {
		return cc == BEANMAP;
	}

	/**
	 * Returns <jk>true</jk> if this class is a subclass of {@link Collection}.
	 *
	 * @return <jk>true</jk> if this class is a subclass of {@link Collection}.
	 */
	public boolean isCollection() {
		return cc == COLLECTION;
	}

	/**
	 * Returns <jk>true</jk> if this class is a subclass of {@link Collection} or is an array.
	 *
	 * @return <jk>true</jk> if this class is a subclass of {@link Collection} or is an array.
	 */
	public boolean isCollectionOrArray() {
		return cc == COLLECTION || cc == ARRAY;
	}

	/**
	 * Returns <jk>true</jk> if this class is {@link Class}.
	 *
	 * @return <jk>true</jk> if this class is {@link Class}.
	 */
	public boolean isClass() {
		return cc == CLASS;
	}

	/**
	 * Returns <jk>true</jk> if this class is an {@link Enum}.
	 *
	 * @return <jk>true</jk> if this class is an {@link Enum}.
	 */
	public boolean isEnum() {
		return cc == ENUM;
	}

	/**
	 * Returns <jk>true</jk> if this class is an array.
	 *
	 * @return <jk>true</jk> if this class is an array.
	 */
	public boolean isArray() {
		return cc == ARRAY;
	}

	/**
	 * Returns <jk>true</jk> if this class is a bean.
	 *
	 * @return <jk>true</jk> if this class is a bean.
	 */
	public boolean isBean() {
		return beanMeta != null;
	}

	/**
	 * Returns <jk>true</jk> if this class is {@link Object}.
	 *
	 * @return <jk>true</jk> if this class is {@link Object}.
	 */
	public boolean isObject() {
		return cc == OBJ;
	}

	/**
	 * Returns <jk>true</jk> if this class is not {@link Object}.
	 *
	 * @return <jk>true</jk> if this class is not {@link Object}.
	 */
	public boolean isNotObject() {
		return cc != OBJ;
	}

	/**
	 * Returns <jk>true</jk> if this class is a subclass of {@link Number}.
	 *
	 * @return <jk>true</jk> if this class is a subclass of {@link Number}.
	 */
	public boolean isNumber() {
		return cc == NUMBER || cc == DECIMAL;
	}

	/**
	 * Returns <jk>true</jk> if this class is a subclass of {@link Float} or {@link Double}.
	 *
	 * @return <jk>true</jk> if this class is a subclass of {@link Float} or {@link Double}.
	 */
	public boolean isDecimal() {
		return cc == DECIMAL;
	}

	/**
	 * Returns <jk>true</jk> if this class is a {@link Boolean}.
	 *
	 * @return <jk>true</jk> if this class is a {@link Boolean}.
	 */
	public boolean isBoolean() {
		return cc == BOOLEAN;
	}

	/**
	 * Returns <jk>true</jk> if this class is a subclass of {@link CharSequence}.
	 *
	 * @return <jk>true</jk> if this class is a subclass of {@link CharSequence}.
	 */
	public boolean isCharSequence() {
		return cc == STR || cc == CHARSEQ;
	}

	/**
	 * Returns <jk>true</jk> if this class is a {@link String}.
	 *
	 * @return <jk>true</jk> if this class is a {@link String}.
	 */
	public boolean isString() {
		return cc == STR;
	}

	/**
	 * Returns <jk>true</jk> if this class is a {@link Character}.
	 *
	 * @return <jk>true</jk> if this class is a {@link Character}.
	 */
	public boolean isChar() {
		return cc == CHAR;
	}

	/**
	 * Returns <jk>true</jk> if this class is a primitive.
	 *
	 * @return <jk>true</jk> if this class is a primitive.
	 */
	public boolean isPrimitive() {
		return innerClass.isPrimitive();
	}

	/**
	 * Returns <jk>true</jk> if this class is a {@link Date} or {@link Calendar}.
	 *
	 * @return <jk>true</jk> if this class is a {@link Date} or {@link Calendar}.
	 */
	public boolean isDate() {
		return cc == DATE;
	}

	/**
	 * Returns <jk>true</jk> if this class is a {@link URI} or {@link URL}.
	 *
	 * @return <jk>true</jk> if this class is a {@link URI} or {@link URL}.
	 */
	public boolean isUri() {
		return cc == URI;
	}

	/**
	 * Returns <jk>true</jk> if this class is a {@link Reader}.
	 *
	 * @return <jk>true</jk> if this class is a {@link Reader}.
	 */
	public boolean isReader() {
		return cc == READER;
	}

	/**
	 * Returns <jk>true</jk> if this class is an {@link InputStream}.
	 *
	 * @return <jk>true</jk> if this class is an {@link InputStream}.
	 */
	public boolean isInputStream() {
		return cc == INPUTSTREAM;
	}

	/**
	 * Returns <jk>true</jk> if instance of this object can be <jk>null</jk>.
	 * <p>
	 * 	Objects can be <jk>null</jk>, but primitives cannot, except for chars which can be represented
	 * 	by <code>(<jk>char</jk>)0</code>.
	 *
	 * @return <jk>true</jk> if instance of this class can be null.
	 */
	public boolean isNullable() {
		if (innerClass.isPrimitive())
			return cc == CHAR;
		return true;
	}

	/**
	 * Returns <jk>true</jk> if this class or one of it's methods are annotated with {@link Remoteable @Remotable}.
	 *
	 * @return <jk>true</jk> if this class is remoteable.
	 */
	public boolean isRemoteable() {
		return remoteableMethods != null;
	}

	/**
	 * All methods on this class annotated with {@link Remoteable @Remotable}, or all public methods if class is annotated.
	 * Keys are method signatures.
	 *
	 * @return All remoteable methods on this class.
	 */
	public Map<String,Method> getRemoteableMethods() {
		return remoteableMethods;
	}

	/**
	 * All public methods on this class including static methods.
	 * Keys are method signatures.
	 *
	 * @return The public methods on this class.
	 */
	public Map<String,Method> getPublicMethods() {
		return publicMethods;
	}

	/**
	 * Returns the {@link PojoSwap} associated with this class.
	 *
	 * @return The {@link PojoSwap} associated with this class, or <jk>null</jk> if there is no POJO swap
	 * 	associated with this class.
	 */
	public PojoSwap<T,?> getPojoSwap() {
		return pojoSwap;
	}

	/**
	 * Returns the {@link BeanMeta} associated with this class.
	 *
	 * @return The {@link BeanMeta} associated with this class, or <jk>null</jk> if there is no bean meta
	 * 	associated with this class.
	 */
	public BeanMeta<T> getBeanMeta() {
		return beanMeta;
	}

	/**
	 * Returns the no-arg constructor for this class.
	 *
	 * @return The no-arg constructor for this class, or <jk>null</jk> if it does not exist.
	 */
	public Constructor<? extends T> getConstructor() {
		return noArgConstructor;
	}

	/**
	 * Returns the language-specified extended metadata on this class.
	 *
	 * @param c The name of the metadata class to create.
	 * @return Extended metadata on this class.  Never <jk>null</jk>.
	 */
	public <M extends ClassMetaExtended> M getExtendedMeta(Class<M> c) {
		return extMeta.get(c, this);
	}

	/**
	 * Returns the interface proxy invocation handler for this class.
	 *
	 * @return The interface proxy invocation handler, or <jk>null</jk> if it does not exist.
	 */
	public InvocationHandler getProxyInvocationHandler() {
		if (invocationHandler == null && beanMeta != null && beanContext.useInterfaceProxies && innerClass.isInterface())
			invocationHandler = new BeanProxyInvocationHandler<T>(beanMeta);
		return invocationHandler;
	}

	/**
	 * Returns <jk>true</jk> if this class has a no-arg constructor or invocation handler.
	 *
	 * @return <jk>true</jk> if a new instance of this class can be constructed.
	 */
	public boolean canCreateNewInstance() {
		if (isMemberClass)
			return false;
		if (noArgConstructor != null)
			return true;
		if (getProxyInvocationHandler() != null)
			return true;
		if (isArray() && elementType.canCreateNewInstance())
			return true;
		return false;
	}

	/**
	 * Returns <jk>true</jk> if this class has a no-arg constructor or invocation handler.
	 * Returns <jk>false</jk> if this is a non-static member class and the outer object does not match
	 * 	the class type of the defining class.
	 *
	 * @param outer The outer class object for non-static member classes.  Can be <jk>null</jk> for non-member or static classes.
	 * @return <jk>true</jk> if a new instance of this class can be created within the context of the specified outer object.
	 */
	public boolean canCreateNewInstance(Object outer) {
		if (isMemberClass)
			return outer != null && noArgConstructor != null && noArgConstructor.getParameterTypes()[0] == outer.getClass();
		return canCreateNewInstance();
	}

	/**
	 * Returns <jk>true</jk> if this class can be instantiated as a bean.
	 * Returns <jk>false</jk> if this is a non-static member class and the outer object does not match
	 * 	the class type of the defining class.
	 *
	 * @param outer The outer class object for non-static member classes.  Can be <jk>null</jk> for non-member or static classes.
	 * @return <jk>true</jk> if a new instance of this bean can be created within the context of the specified outer object.
	 */
	public boolean canCreateNewBean(Object outer) {
		if (beanMeta == null)
			return false;
		// Beans with transforms with subtype properties are assumed to be constructable.
		if (beanFilter != null && beanFilter.getSubTypeProperty() != null)
			return true;
		if (beanMeta.constructor == null)
			return false;
		if (isMemberClass)
			return outer != null && beanMeta.constructor.getParameterTypes()[0] == outer.getClass();
		return true;
	}

	/**
	 * Returns <jk>true</jk> if this class can call the {@link #newInstanceFromString(Object, String)} method.
	 *
	 * @param outer The outer class object for non-static member classes.  Can be <jk>null</jk> for non-member or static classes.
	 * @return <jk>true</jk> if this class has a no-arg constructor or invocation handler.
	 */
	public boolean canCreateNewInstanceFromString(Object outer) {
		if (fromStringMethod != null)
			return true;
		if (stringConstructor != null) {
			if (isMemberClass)
				return outer != null && stringConstructor.getParameterTypes()[0] == outer.getClass();
			return true;
		}
		return false;
	}

	/**
	 * Returns <jk>true</jk> if this class can call the {@link #newInstanceFromString(Object, String)} method.
	 *
	 * @param outer The outer class object for non-static member classes.  Can be <jk>null</jk> for non-member or static classes.
	 * @return <jk>true</jk> if this class has a no-arg constructor or invocation handler.
	 */
	public boolean canCreateNewInstanceFromNumber(Object outer) {
		if (numberConstructor != null) {
			if (isMemberClass)
				return outer != null && numberConstructor.getParameterTypes()[0] == outer.getClass();
			return true;
		}
		return false;
	}

	/**
	 * Returns the class type of the parameter of the numeric constructor.
	 *
	 * @return The class type of the numeric constructor, or <jk>null</jk> if no such constructor exists.
	 */
	@SuppressWarnings("unchecked")
	public Class<? extends Number> getNewInstanceFromNumberClass() {
		return (Class<? extends Number>) numberConstructorType;
	}

	/**
	 * Returns <jk>true</jk> if this class can call the {@link #newInstanceFromString(Object, String)} method.
	 *
	 * @param outer The outer class object for non-static member classes.  Can be <jk>null</jk> for non-member or static classes.
	 * @return <jk>true</jk> if this class has a no-arg constructor or invocation handler.
	 */
	public boolean canCreateNewInstanceFromObjectMap(Object outer) {
		if (objectMapConstructor != null) {
			if (isMemberClass)
				return outer != null && objectMapConstructor.getParameterTypes()[0] == outer.getClass();
			return true;
		}
		return false;
	}

	/**
	 * Returns <jk>true</jk> if this class has an <code>ObjectMap toObjectMap()</code> method.
	 *
	 * @return <jk>true</jk> if class has a <code>toObjectMap()</code> method.
	 */
	public boolean hasToObjectMapMethod() {
		return toObjectMapMethod != null;
	}

	/**
	 * Returns the method annotated with {@link NameProperty @NameProperty}.
	 *
	 * @return The method annotated with {@link NameProperty @NameProperty} or <jk>null</jk> if method does not exist.
	 */
	public Method getNameProperty() {
		return namePropertyMethod;
 	}

	/**
	 * Returns the method annotated with {@link ParentProperty @ParentProperty}.
	 *
	 * @return The method annotated with {@link ParentProperty @ParentProperty} or <jk>null</jk> if method does not exist.
	 */
	public Method getParentProperty() {
		return parentPropertyMethod;
 	}

	/**
	 * Converts an instance of this class to an {@link ObjectMap}.
	 *
	 * @param t The object to convert to a map.
	 * @return The converted object, or <jk>null</jk> if method does not have a <code>toObjectMap()</code> method.
	 * @throws BeanRuntimeException Thrown by <code>toObjectMap()</code> method invocation.
	 */
	public ObjectMap toObjectMap(Object t) throws BeanRuntimeException {
		try {
			if (toObjectMapMethod != null)
				return (ObjectMap)toObjectMapMethod.invoke(t);
			return null;
		} catch (Exception e) {
			throw new BeanRuntimeException(e);
		}
	}

	/**
	 * Returns the reason why this class is not a bean, or <jk>null</jk> if it is a bean.
	 *
	 * @return The reason why this class is not a bean, or <jk>null</jk> if it is a bean.
	 */
	public synchronized String getNotABeanReason() {
		return notABeanReason;
	}

	/**
	 * Returns <jk>true</jk> if this class is abstract.
	 * @return <jk>true</jk> if this class is abstract.
	 */
	public boolean isAbstract() {
		return isAbstract;
	}

	/**
	 * Returns any exception that was throw in the <code>init()</code> method.
	 *
	 * @return The cached exception.
	 */
	public Throwable getInitException() {
		return initException;
	}

	/**
	 * Returns the {@link BeanContext} that created this object.
	 *
	 * @return The bean context.
	 */
	public BeanContext getBeanContext() {
		return beanContext;
	}

	/**
	 * Returns the default value for primitives such as <jk>int</jk> or <jk>Integer</jk>.
	 *
	 * @return The default value, or <jk>null</jk> if this class type is not a primitive.
	 */
	@SuppressWarnings("unchecked")
	public T getPrimitiveDefault() {
		return (T)primitiveDefault;
	}

	/**
	 * Create a new instance of the main class of this declared type from a <code>String</code> input.
	 * <p>
	 * In order to use this method, the class must have one of the following methods:
	 * <ul>
	 * 	<li><code><jk>public static</jk> T valueOf(String in);</code>
	 * 	<li><code><jk>public static</jk> T fromString(String in);</code>
	 * 	<li><code><jk>public</jk> T(String in);</code>
	 * </ul>
	 *
	 * @param outer The outer class object for non-static member classes.  Can be <jk>null</jk> for non-member or static classes.
	 * @param arg The input argument value.
	 * @return A new instance of the object, or <jk>null</jk> if there is no string constructor on the object.
	 * @throws IllegalAccessException If the <code>Constructor</code> object enforces Java language access control and the underlying constructor is inaccessible.
	 * @throws IllegalArgumentException If the parameter type on the method was invalid.
	 * @throws InstantiationException If the class that declares the underlying constructor represents an abstract class, or
	 * 	does not have one of the methods described above.
	 * @throws InvocationTargetException If the underlying constructor throws an exception.
	 */
	@SuppressWarnings("unchecked")
	public T newInstanceFromString(Object outer, String arg) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, InstantiationException {
		Method m = fromStringMethod;
		if (m != null)
			return (T)m.invoke(null, arg);
		Constructor<T> c = stringConstructor;
		if (c != null) {
			if (isMemberClass)
				return c.newInstance(outer, arg);
			return c.newInstance(arg);
		}
		throw new InstantiationError("No string constructor or valueOf(String) method found for class '"+getInnerClass().getName()+"'");
	}

	/**
	 * Create a new instance of the main class of this declared type from a <code>Number</code> input.
	 * <p>
	 * In order to use this method, the class must have one of the following methods:
	 * <ul>
	 * 	<li><code><jk>public</jk> T(Number in);</code>
	 * </ul>
	 *
	 * @param session The current bean session.
	 * @param outer The outer class object for non-static member classes.  Can be <jk>null</jk> for non-member or static classes.
	 * @param arg The input argument value.
	 * @return A new instance of the object, or <jk>null</jk> if there is no numeric constructor on the object.
	 * @throws IllegalAccessException If the <code>Constructor</code> object enforces Java language access control and the underlying constructor is inaccessible.
	 * @throws IllegalArgumentException If the parameter type on the method was invalid.
	 * @throws InstantiationException If the class that declares the underlying constructor represents an abstract class, or
	 * 	does not have one of the methods described above.
	 * @throws InvocationTargetException If the underlying constructor throws an exception.
	 */
	public T newInstanceFromNumber(BeanSession session, Object outer, Number arg) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, InstantiationException {
		Constructor<T> c = numberConstructor;
		if (c != null) {
			Object arg2 = session.convertToType(arg, numberConstructor.getParameterTypes()[0]);
			if (isMemberClass)
				return c.newInstance(outer, arg2);
			return c.newInstance(arg2);
		}
		throw new InstantiationError("No string constructor or valueOf(Number) method found for class '"+getInnerClass().getName()+"'");
	}

	/**
	 * Create a new instance of the main class of this declared type from an <code>ObjectMap</code> input.
	 * <p>
	 * In order to use this method, the class must have one of the following methods:
	 * <ul>
	 * 	<li><code><jk>public</jk> T(ObjectMap in);</code>
	 * </ul>
	 *
	 * @param outer The outer class object for non-static member classes.  Can be <jk>null</jk> for non-member or static classes.
	 * @param arg The input argument value.
	 * @return A new instance of the object.
	 * @throws IllegalAccessException If the <code>Constructor</code> object enforces Java language access control and the underlying constructor is inaccessible.
	 * @throws IllegalArgumentException If the parameter type on the method was invalid.
	 * @throws InstantiationException If the class that declares the underlying constructor represents an abstract class, or
	 * 	does not have one of the methods described above.
	 * @throws InvocationTargetException If the underlying constructor throws an exception.
	 */
	public T newInstanceFromObjectMap(Object outer, ObjectMap arg) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, InstantiationException {
		Constructor<T> c = objectMapConstructor;
		if (c != null) {
			if (isMemberClass)
				return c.newInstance(outer, arg);
			return c.newInstance(arg);
		}
		throw new InstantiationError("No map constructor method found for class '"+getInnerClass().getName()+"'");
	}

	/**
	 * Create a new instance of the main class of this declared type.
	 *
	 * @return A new instance of the object, or <jk>null</jk> if there is no no-arg constructor on the object.
	 * @throws IllegalAccessException If the <code>Constructor</code> object enforces Java language access control and the underlying constructor is inaccessible.
	 * @throws IllegalArgumentException If one of the following occurs:
	 * 	<ul class='spaced-list'>
	 * 		<li>The number of actual and formal parameters differ.
	 * 		<li>An unwrapping conversion for primitive arguments fails.
	 * 		<li>A parameter value cannot be converted to the corresponding formal parameter type by a method invocation conversion.
	 * 		<li>The constructor pertains to an enum type.
	 * 	</ul>
	 * @throws InstantiationException If the class that declares the underlying constructor represents an abstract class.
	 * @throws InvocationTargetException If the underlying constructor throws an exception.
	 */
	@SuppressWarnings("unchecked")
	public T newInstance() throws IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
		if (isArray())
			return (T)Array.newInstance(getInnerClass().getComponentType(), 0);
		Constructor<? extends T> c = getConstructor();
		if (c != null)
			return c.newInstance((Object[])null);
		InvocationHandler h = getProxyInvocationHandler();
		if (h != null)
			return (T)Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] { getInnerClass(), java.io.Serializable.class }, h);
		if (isArray())
			return (T)Array.newInstance(this.elementType.innerClass,0);
		return null;
	}

	/**
	 * Same as {@link #newInstance()} except for instantiating non-static member classes.
	 *
	 * @param outer The instance of the owning object of the member class instance.  Can be <jk>null</jk> if instantiating a non-member or static class.
	 * @return A new instance of the object, or <jk>null</jk> if there is no no-arg constructor on the object.
	 * @throws IllegalAccessException If the <code>Constructor</code> object enforces Java language access control and the underlying constructor is inaccessible.
	 * @throws IllegalArgumentException If one of the following occurs:
	 * 	<ul class='spaced-list'>
	 * 		<li>The number of actual and formal parameters differ.
	 * 		<li>An unwrapping conversion for primitive arguments fails.
	 * 		<li>A parameter value cannot be converted to the corresponding formal parameter type by a method invocation conversion.
	 * 		<li>The constructor pertains to an enum type.
	 * 	</ul>
	 * @throws InstantiationException If the class that declares the underlying constructor represents an abstract class.
	 * @throws InvocationTargetException If the underlying constructor throws an exception.
	 */
	public T newInstance(Object outer) throws IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
		if (isMemberClass)
			return noArgConstructor.newInstance(outer);
		return newInstance();
	}

	/**
	 * Checks to see if the specified class type is the same as this one.
	 *
	 * @param t The specified class type.
	 * @return <jk>true</jk> if the specified class type is the same as the class for this type.
	 */
	@Override /* Object */
	public boolean equals(Object t) {
		if (t == null || ! (t instanceof ClassMeta))
			return false;
		ClassMeta<?> t2 = (ClassMeta<?>)t;
		return t2.getInnerClass() == this.getInnerClass();
	}

	/**
	 * Similar to {@link #equals(Object)} except primitive and Object types that are similar
	 * are considered the same. (e.g. <jk>boolean</jk> == <code>Boolean</code>).
	 *
	 * @param cm The class meta to compare to.
	 * @return <jk>true</jk> if the specified class-meta is equivalent to this one.
	 */
	public boolean same(ClassMeta<?> cm) {
		if (equals(cm))
			return true;
		return (isPrimitive() && cc == cm.cc);
	}

	@Override /* Object */
	public String toString() {
		return toString(false);
	}

	/**
	 * Same as {@link #toString()} except use simple class names.
	 *
	 * @param simple Print simple class names only (no package).
	 * @return A new string.
	 */
	public String toString(boolean simple) {
		return toString(new StringBuilder(), simple).toString();
	}

	/**
	 * Appends this object as a readable string to the specified string builder.
	 *
	 * @param sb The string builder to append this object to.
	 * @param simple Print simple class names only (no package).
	 * @return The same string builder passed in (for method chaining).
	 */
	protected StringBuilder toString(StringBuilder sb, boolean simple) {
		String n = innerClass.getName();
		if (simple) {
			int i = n.lastIndexOf('.');
			n = n.substring(i == -1 ? 0 : i+1).replace('$', '.');
		}
		if (cc == ARRAY)
			return elementType.toString(sb, simple).append('[').append(']');
		if (cc == MAP)
			return sb.append(n).append(keyType.isObject() && valueType.isObject() ? "" : "<"+keyType.toString(simple)+","+valueType.toString(simple)+">");
		if (cc == BEANMAP)
			return sb.append(BeanMap.class.getName()).append('<').append(n).append('>');
		if (cc == COLLECTION)
			return sb.append(n).append(elementType.isObject() ? "" : "<"+elementType.toString(simple)+">");
		if (cc == OTHER && beanMeta == null) {
			if (simple)
				return sb.append(n);
			sb.append("OTHER-").append(n).append(",notABeanReason=").append(notABeanReason);
			if (initException != null)
				sb.append(",initException=").append(initException);
			return sb;
		}
		return sb.append(n);
	}

	/**
	 * Returns <jk>true</jk> if the specified object is an instance of this class.
	 * This is a simple comparison on the base class itself and not on
	 * any generic parameters.
	 *
	 * @param o The object to check.
	 * @return <jk>true</jk> if the specified object is an instance of this class.
	 */
	public boolean isInstance(Object o) {
		if (o != null)
			return ClassUtils.isParentClass(this.innerClass, o.getClass());
		return false;
	}

	/**
	 * Returns a readable name for this class (e.g. <js>"java.lang.String"</js>, <js>"boolean[]"</js>).
	 *
	 * @return The readable name for this class.
	 */
	public String getReadableName() {
		return ClassUtils.getReadableClassName(this.innerClass);
	}

	private static class LocaleAsString {
		private static Method forLanguageTagMethod;
		static {
			try {
				forLanguageTagMethod = Locale.class.getMethod("forLanguageTag", String.class);
			} catch (NoSuchMethodException e) {}
		}

		@SuppressWarnings("unused")
		public static final Locale fromString(String localeString) {
			if (forLanguageTagMethod != null) {
				if (localeString.indexOf('_') != -1)
					localeString = localeString.replace('_', '-');
				try {
					return (Locale)forLanguageTagMethod.invoke(null, localeString);
				} catch (Exception e) {
					throw new BeanRuntimeException(e);
				}
			}
			String[] v = localeString.toString().split("[\\-\\_]");
			if (v.length == 1)
				return new Locale(v[0]);
			else if (v.length == 2)
				return new Locale(v[0], v[1]);
			else if (v.length == 3)
				return new Locale(v[0], v[1], v[2]);
			throw new BeanRuntimeException("Could not convert string ''{0}'' to a Locale.", localeString);
		}
	}

	@Override /* Object */
	public int hashCode() {
		return super.hashCode();
	}
//
//	public abstract static class CreateSession {
//		LinkedList<ClassMeta<?>> stack;
//
//		public CreateSession push(ClassMeta<?> cm) {
//			if (stack == null)
//				stack = new LinkedList<ClassMeta<?>>();
//			stack.add(cm);
//			return this;
//		}
//
//		public CreateSession pop(ClassMeta<?> expected) {
//			if (stack == null || stack.removeLast() != expected)
//				throw new BeanRuntimeException("ClassMetaSession creation stack corruption!");
//			return this;
//		}
//
//		public <T> ClassMeta<T> getClassMeta(Class<T> c) {
//			if (stack != null)
//				for (ClassMeta<?> cm : stack)
//					if (cm.innerClass == c)
//						return (ClassMeta<T>)cm;
//			return createClassMeta(c);
//		}
//
//		public abstract <T> ClassMeta<T> createClassMeta(Class<T> c);
//	}
}
