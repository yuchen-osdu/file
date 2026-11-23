package org.opengroup.osdu.file.coreplus.provider.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.file.DriverType;
import org.opengroup.osdu.core.common.model.file.FileListRequest;
import org.opengroup.osdu.core.common.model.file.FileListResponse;
import org.opengroup.osdu.core.common.model.file.FileLocation;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.osm.core.model.Destination;
import org.opengroup.osdu.core.osm.core.model.order.OrderByElement;
import org.opengroup.osdu.core.osm.core.model.query.GetQuery;
import org.opengroup.osdu.core.osm.core.model.where.condition.And;
import org.opengroup.osdu.core.osm.core.model.where.predicate.Eq;
import org.opengroup.osdu.core.osm.core.model.where.predicate.Ge;
import org.opengroup.osdu.core.osm.core.model.where.predicate.Le;
import org.opengroup.osdu.core.osm.core.model.where.predicate.Predicate;
import org.opengroup.osdu.core.osm.core.service.Context;
import org.opengroup.osdu.file.coreplus.config.CorePlusConfigurationProperties;
import org.opengroup.osdu.file.coreplus.model.FileLocationOsm;
import org.opengroup.osdu.file.exception.FileLocationNotFoundException;

@ExtendWith(MockitoExtension.class)
class OsmFileLocationRepositoryTest {

  @Mock private Context database;
  @Mock private JaxRsDpsLog log;
  @Captor private ArgumentCaptor<GetQuery<FileLocationOsm>> query;
  private OsmFileLocationRepository repository;

  @BeforeEach
  void setUp() {
    CorePlusConfigurationProperties properties = new CorePlusConfigurationProperties();
    properties.setFileLocationKind("file-location");
    TenantInfo tenant = new TenantInfo();
    tenant.setDataPartitionId("opendes");
    tenant.setName("tenant");
    repository = new OsmFileLocationRepository(database, properties, tenant, log);
  }

  @Test
  void nullIdentifierDoesNotQueryDatabase() {
    assertNull(repository.findByFileID(null));
    verifyNoInteractions(database);
  }

  @Test
  void lookupScopesQueryToTenantAndReturnsFirstMatch() {
    FileLocation first = location("file-1");
    when(database.getResultsAsList(any(GetQuery.class))).thenReturn(List.of(
        new FileLocationOsm(first, 1L), new FileLocationOsm(location("file-2"), 2L)));

    assertEquals(first, repository.findByFileID("file-1"));

    verify(database).getResultsAsList(query.capture());
    assertTenantDestination(query.getValue().getDestination());
    assertEquals(FileLocationOsm.class, query.getValue().getEntityType());
    Eq filter = assertInstanceOf(Eq.class, query.getValue().getWhere());
    assertEquals("fileID", filter.getField());
    assertEquals("file-1", filter.getValue());
  }

  @Test
  void missingIdentifierReturnsNull() {
    when(database.getResultsAsList(any(GetQuery.class))).thenReturn(List.of());

    assertNull(repository.findByFileID("missing"));
  }

  @Test
  void savePersistsAllMetadataAndReturnsTheDatabaseResult() {
    FileLocation input = location("file-1");
    FileLocation persisted = location("file-1");
    persisted.setLocation("s3://persistent/file-1");
    when(database.createAndGet(any(Destination.class), any(FileLocationOsm.class)))
        .thenReturn(new FileLocationOsm(persisted, 99L));

    assertEquals(persisted, repository.save(input));

    ArgumentCaptor<FileLocationOsm> entity = ArgumentCaptor.forClass(FileLocationOsm.class);
    ArgumentCaptor<Destination> destination = ArgumentCaptor.forClass(Destination.class);
    verify(database).createAndGet(destination.capture(), entity.capture());
    assertTenantDestination(destination.getValue());
    assertEquals(input, entity.getValue().toFileLocation());
    assertEquals(input.getCreatedAt().getTime(), entity.getValue().getCreatedAt());
    assertNotNull(entity.getValue().getId());
  }

  @Test
  void listUsesInclusiveUtcTimeAndUserFiltersThenPaginatesInOrder() {
    FileListRequest request = request();
    when(database.getResultsAsList(any(GetQuery.class))).thenReturn(List.of(
        new FileLocationOsm(location("first"), 1L),
        new FileLocationOsm(location("second"), 2L),
        new FileLocationOsm(location("third"), 3L)));

    FileListResponse result = repository.findAll(request);

    assertEquals(List.of(location("third")), result.getContent());
    assertEquals(2, result.getSize());
    assertEquals(1, result.getNumber());
    assertEquals(3, result.getNumberOfElements());
    verify(database).getResultsAsList(query.capture());
    assertTenantDestination(query.getValue().getDestination());
    And condition = assertInstanceOf(And.class, query.getValue().getWhere());
    assertEquals(3, condition.getCondition().size());
    Predicate from = assertInstanceOf(Ge.class, condition.getCondition().get(0));
    Predicate to = assertInstanceOf(Le.class, condition.getCondition().get(1));
    Predicate user = assertInstanceOf(Eq.class, condition.getCondition().get(2));
    assertEquals("createdAt", from.getField());
    assertEquals(request.getTimeFrom().toInstant(ZoneOffset.UTC).toEpochMilli(), from.getValue());
    assertEquals("createdAt", to.getField());
    assertEquals(request.getTimeTo().toInstant(ZoneOffset.UTC).toEpochMilli(), to.getValue());
    assertEquals("createdBy", user.getField());
    assertEquals("user@example.com", user.getValue());
    List<OrderByElement> order = query.getValue().getOrderBy().getOrderByElements();
    assertEquals(1, order.size());
    assertEquals("createdAt", order.getFirst().getField());
    assertFalse(order.getFirst().isDescending());
  }

  @Test
  void emptyListReportsRequestedPage() {
    when(database.getResultsAsList(any(GetQuery.class))).thenReturn(List.of());

    FileLocationNotFoundException error =
        assertThrows(FileLocationNotFoundException.class, () -> repository.findAll(request()));

    assertEquals("Nothing found for such filter and page(num: 1, size: 2).", error.getMessage());
  }

  private FileListRequest request() {
    return FileListRequest.builder().pageNum(1).items((short) 2).userID("user@example.com")
        .timeFrom(LocalDateTime.of(2026, 1, 1, 0, 0))
        .timeTo(LocalDateTime.of(2026, 1, 2, 0, 0)).build();
  }

  private void assertTenantDestination(Destination destination) {
    assertEquals("opendes", destination.getPartitionId());
    assertEquals("tenant", destination.getNamespace().getName());
    assertEquals("file-location", destination.getKind().getName());
  }

  private FileLocation location(String id) {
    return FileLocation.builder().fileID(id).driver(DriverType.GCS).location("s3://staging/" + id)
        .createdAt(Date.from(Instant.parse("2026-01-01T12:30:45.123Z")))
        .createdBy("user@example.com").build();
  }
}
