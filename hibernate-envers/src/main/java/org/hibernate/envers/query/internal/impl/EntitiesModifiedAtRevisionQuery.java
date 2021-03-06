/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.query.internal.impl;

import java.util.List;

import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.configuration.internal.AuditEntitiesConfiguration;
import org.hibernate.envers.internal.entities.mapper.relation.query.QueryConstants;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.query.criteria.AuditCriterion;
import org.hibernate.query.Query;

/**
 * In comparison to {@link EntitiesAtRevisionQuery} this query returns an empty collection if an entity
 * of a certain type has not been changed in a given revision.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 * @see EntitiesAtRevisionQuery
 */
public class EntitiesModifiedAtRevisionQuery extends AbstractAuditQuery {
	private final Number revision;

	public EntitiesModifiedAtRevisionQuery(
			EnversService enversService,
			AuditReaderImplementor versionsReader,
			Class<?> cls,
			Number revision) {
		super( enversService, versionsReader, cls );
		this.revision = revision;
	}

	public EntitiesModifiedAtRevisionQuery(
			EnversService enversService,
			AuditReaderImplementor versionsReader,
			Class<?> cls,
			String entityName,
			Number revision) {
		super( enversService, versionsReader, cls, entityName );
		this.revision = revision;
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public List list() {
		/*
         * The query that we need to create:
         *   SELECT new list(e) FROM versionsReferencedEntity e
         *   WHERE
         * (all specified conditions, transformed, on the "e" entity) AND
         * e.revision = :revision
         */
		AuditEntitiesConfiguration verEntCfg = enversService.getAuditEntitiesConfiguration();
		String revisionPropertyPath = verEntCfg.getRevisionNumberPath();
		qb.getRootParameters().addWhereWithParam( revisionPropertyPath, "=", revision );

		// all specified conditions
		for ( AuditCriterion criterion : criterions ) {
			criterion.addToQuery(
					enversService,
					versionsReader,
					entityName,
					QueryConstants.REFERENCED_ENTITY_ALIAS,
					qb,
					qb.getRootParameters()
			);
		}

		for (final AuditAssociationQueryImpl<?> associationQuery : associationQueries) {
			associationQuery.addCriterionsToQuery( versionsReader );
		}

		Query query = buildQuery();
		List queryResult = query.list();
		return applyProjections( queryResult, revision );
	}
}
