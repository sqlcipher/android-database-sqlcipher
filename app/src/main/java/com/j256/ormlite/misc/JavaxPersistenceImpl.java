package com.j256.ormlite.misc;

import java.lang.reflect.Field;
import java.util.Collection;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.db.DatabaseType;
import com.j256.ormlite.field.DataPersisterManager;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseFieldConfig;

/**
 * Class for isolating the detection of the javax.persistence annotations. This used to be a hard dependency but it has
 * become optinal/test since we use reflection here.
 * 
 * @author graywatson
 */
public class JavaxPersistenceImpl implements JavaxPersistenceConfigurer {

	@Override
	public DatabaseFieldConfig createFieldConfig(DatabaseType databaseType, Field field) {
		Column columnAnnotation = field.getAnnotation(Column.class);
		Basic basicAnnotation = field.getAnnotation(Basic.class);
		Id idAnnotation = field.getAnnotation(Id.class);
		GeneratedValue generatedValueAnnotation = field.getAnnotation(GeneratedValue.class);
		OneToOne oneToOneAnnotation = field.getAnnotation(OneToOne.class);
		ManyToOne manyToOneAnnotation = field.getAnnotation(ManyToOne.class);
		JoinColumn joinColumnAnnotation = field.getAnnotation(JoinColumn.class);
		Enumerated enumeratedAnnotation = field.getAnnotation(Enumerated.class);
		Version versionAnnotation = field.getAnnotation(Version.class);

		if (columnAnnotation == null && basicAnnotation == null && idAnnotation == null && oneToOneAnnotation == null
				&& manyToOneAnnotation == null && enumeratedAnnotation == null && versionAnnotation == null) {
			return null;
		}

		DatabaseFieldConfig config = new DatabaseFieldConfig();
		String fieldName = field.getName();
		if (databaseType.isEntityNamesMustBeUpCase()) {
			fieldName = databaseType.upCaseEntityName(fieldName);
		}
		config.setFieldName(fieldName);

		if (columnAnnotation != null) {
			if (stringNotEmpty(columnAnnotation.name())) {
				config.setColumnName(columnAnnotation.name());
			}
			if (stringNotEmpty(columnAnnotation.columnDefinition())) {
				config.setColumnDefinition(columnAnnotation.columnDefinition());
			}
			config.setWidth(columnAnnotation.length());
			config.setCanBeNull(columnAnnotation.nullable());
			config.setUnique(columnAnnotation.unique());
		}
		if (basicAnnotation != null) {
			config.setCanBeNull(basicAnnotation.optional());
		}
		if (idAnnotation != null) {
			if (generatedValueAnnotation == null) {
				config.setId(true);
			} else {
				// generatedValue only works if it is also an id according to {@link GeneratedValue)
				config.setGeneratedId(true);
			}
		}
		if (oneToOneAnnotation != null || manyToOneAnnotation != null) {
			// if we have a collection then make it a foreign collection
			if (Collection.class.isAssignableFrom(field.getType())
					|| ForeignCollection.class.isAssignableFrom(field.getType())) {
				config.setForeignCollection(true);
				if (joinColumnAnnotation != null && stringNotEmpty(joinColumnAnnotation.name())) {
					config.setForeignCollectionColumnName(joinColumnAnnotation.name());
				}
				if (manyToOneAnnotation != null) {
					FetchType fetchType = manyToOneAnnotation.fetch();
					if (fetchType != null && fetchType == FetchType.EAGER) {
						config.setForeignCollectionEager(true);
					}
				}
			} else {
				// otherwise it is a foreign field
				config.setForeign(true);
				if (joinColumnAnnotation != null) {
					if (stringNotEmpty(joinColumnAnnotation.name())) {
						config.setColumnName(joinColumnAnnotation.name());
					}
					config.setCanBeNull(joinColumnAnnotation.nullable());
					config.setUnique(joinColumnAnnotation.unique());
				}
			}
		}
		if (enumeratedAnnotation != null) {
			EnumType enumType = enumeratedAnnotation.value();
			if (enumType != null && enumType == EnumType.STRING) {
				config.setDataType(DataType.ENUM_STRING);
			} else {
				config.setDataType(DataType.ENUM_INTEGER);
			}
		}
		if (versionAnnotation != null) {
			// just the presence of the version...
			config.setVersion(true);
		}
		if (config.getDataPersister() == null) {
			config.setDataPersister(DataPersisterManager.lookupForField(field));
		}
		config.setUseGetSet(DatabaseFieldConfig.findGetMethod(field, false) != null
				&& DatabaseFieldConfig.findSetMethod(field, false) != null);
		return config;
	}

	@Override
	public String getEntityName(Class<?> clazz) {
		Entity entityAnnotation = clazz.getAnnotation(Entity.class);
		Table tableAnnotation = clazz.getAnnotation(Table.class);

		if (entityAnnotation != null && stringNotEmpty(entityAnnotation.name())) {
			return entityAnnotation.name();
		}
		if (tableAnnotation != null && stringNotEmpty(tableAnnotation.name())) {
			return tableAnnotation.name();
		}
		return null;
	}

	private boolean stringNotEmpty(String value) {
		return (value != null && value.length() > 0);
	}
}
