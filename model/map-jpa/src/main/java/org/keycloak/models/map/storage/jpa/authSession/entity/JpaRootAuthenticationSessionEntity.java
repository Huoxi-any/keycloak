/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.models.map.storage.jpa.authSession.entity;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.keycloak.models.map.authSession.MapAuthenticationSessionEntity;
import org.keycloak.models.map.authSession.MapRootAuthenticationSessionEntity.AbstractRootAuthenticationSessionEntity;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.UuidValidator;
import org.keycloak.models.map.storage.jpa.Constants;
import org.keycloak.models.map.storage.jpa.JpaRootVersionedEntity;
import org.keycloak.models.map.storage.jpa.hibernate.jsonb.JsonbType;

import static org.keycloak.models.map.storage.jpa.JpaMapStorageProviderFactory.CLONER;

/**
 * Entity represents root authentication session.
 * 
 * There are some fields marked by {@code @Column(insertable = false, updatable = false)}.
 * Those fields are automatically generated by database from json field,
 * therefore marked as non-insertable and non-updatable to instruct hibernate.
 */
@Entity
@Table(name = "kc_auth_root_session")
@TypeDefs({@TypeDef(name = "jsonb", typeClass = JsonbType.class)})
public class JpaRootAuthenticationSessionEntity extends AbstractRootAuthenticationSessionEntity implements JpaRootVersionedEntity {

    @Id
    @Column
    private UUID id;

    //used for implicit optimistic locking
    @Version
    @Column
    private int version;

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private final JpaRootAuthenticationSessionMetadata metadata;

    @Column(insertable = false, updatable = false)
    @Basic(fetch = FetchType.LAZY)
    private Integer entityVersion;

    @Column(insertable = false, updatable = false)
    @Basic(fetch = FetchType.LAZY)
    private String realmId;

    @Column(insertable = false, updatable = false)
    @Basic(fetch = FetchType.LAZY)
    private Long timestamp;

    @Column(insertable = false, updatable = false)
    @Basic(fetch = FetchType.LAZY)
    private Long expiration;

    @OneToMany(mappedBy = "root", cascade = CascadeType.PERSIST, orphanRemoval = true)
    private final Set<JpaAuthenticationSessionEntity> authSessions = new HashSet<>();

    /**
     * No-argument constructor, used by hibernate to instantiate entities.
     */
    public JpaRootAuthenticationSessionEntity() {
        this.metadata = new JpaRootAuthenticationSessionMetadata();
    }

    public JpaRootAuthenticationSessionEntity(DeepCloner cloner) {
        this.metadata = new JpaRootAuthenticationSessionMetadata(cloner);
    }

    /**
     * Used by hibernate when calling cb.construct from read(QueryParameters) method.
     * It is used to select root auth session without metadata(json) field.
     */
    public JpaRootAuthenticationSessionEntity(UUID id, Integer entityVersion, String realmId, Long timestamp, Long expiration) {
        this.id = id;
        this.entityVersion = entityVersion;
        this.realmId = realmId;
        this.timestamp = timestamp;
        this.expiration = expiration;
        this.metadata = null;
    }

    public boolean isMetadataInitialized() {
        return metadata != null;
    }

    @Override
    public Integer getEntityVersion() {
        if (isMetadataInitialized()) return metadata.getEntityVersion();
        return entityVersion;
    }

    @Override
    public void setEntityVersion(Integer entityVersion) {
        metadata.setEntityVersion(entityVersion);
    }

    @Override
    public Integer getCurrentSchemaVersion() {
        return Constants.CURRENT_SCHEMA_VERSION_ROOT_AUTH_SESSION;
    }

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public String getId() {
        return id == null ? null : id.toString();
    }

    @Override
    public void setId(String id) {
        String validatedId = UuidValidator.validateAndConvert(id);
        this.id = UUID.fromString(validatedId);
    }

    @Override
    public String getRealmId() {
        if (isMetadataInitialized()) return metadata.getRealmId();
        return realmId;
    }

    @Override
    public void setRealmId(String realmId) {
        metadata.setRealmId(realmId);
    }

    @Override
    public Long getTimestamp() {
        if (isMetadataInitialized()) return metadata.getTimestamp();
        return timestamp;
    }

    @Override
    public void setTimestamp(Long timestamp) {
        metadata.setTimestamp(timestamp);
    }

    @Override
    public Long getExpiration() {
        if (isMetadataInitialized()) return metadata.getExpiration();
        return expiration;
    }

    @Override
    public void setExpiration(Long expiration) {
        metadata.setExpiration(expiration);
    }

    @Override
    public Set<MapAuthenticationSessionEntity> getAuthenticationSessions() {
        return authSessions.stream().map(MapAuthenticationSessionEntity.class::cast).collect(Collectors.toSet());
    }

    @Override
    public void setAuthenticationSessions(Set<MapAuthenticationSessionEntity> authenticationSessions) {
        authSessions.clear();
        if (authenticationSessions != null) {
            for (MapAuthenticationSessionEntity authenticationSession : authenticationSessions) {
                addAuthenticationSession(authenticationSession);
            }
        }
    }

    @Override
    public void addAuthenticationSession(MapAuthenticationSessionEntity authenticationSession) {
        JpaAuthenticationSessionEntity jpaAuthSession = JpaAuthenticationSessionEntity.class.cast(CLONER.from(authenticationSession));
        jpaAuthSession.setParent(this);
        jpaAuthSession.setEntityVersion(Constants.CURRENT_SCHEMA_VERSION_AUTH_SESSION);
        authSessions.add(jpaAuthSession);
    }

    @Override
    public Optional<MapAuthenticationSessionEntity> getAuthenticationSession(String tabId) {
        return authSessions.stream().filter(as -> Objects.equals(as.getTabId(), tabId)).findFirst().map(MapAuthenticationSessionEntity.class::cast);
    }

    @Override
    public Boolean removeAuthenticationSession(String tabId) {
        return authSessions.removeIf(as -> Objects.equals(as.getTabId(), tabId));
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof JpaRootAuthenticationSessionEntity)) return false;
        return Objects.equals(getId(), ((JpaRootAuthenticationSessionEntity) obj).getId());
    }
}
