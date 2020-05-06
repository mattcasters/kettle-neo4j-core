package org.neo4j.kettle.model.cw;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.kettle.model.GraphModel;
import org.neo4j.kettle.model.GraphNode;
import org.neo4j.kettle.model.GraphPresentation;
import org.neo4j.kettle.model.GraphProperty;
import org.neo4j.kettle.model.GraphRelationship;
import org.pentaho.di.core.exception.KettleException;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class CypherWorkbenchBeerImporterTest extends CypherWorkbenchBaseImporterTest {

  @Before
  public void setUp() throws Exception {
    String jsonString = FileUtils.readFileToString( new File( "src/test/resources/cw/cw-beers.json" ), "UTF-8" );
    graphModel = CypherWorkbenchImporter.importFromCwJson( jsonString );
  }

  @Test
  public void importFromCwJson() throws Exception {
    assertEquals( "model name is not taken from CW model title", "Beers", graphModel.getName() );
    assertEquals( "model description is not taken from CW model description", "Testing", graphModel.getDescription() );

    List<GraphNode> nodes = graphModel.getNodes();
    assertEquals( "Not all nodes were imported", 4, nodes.size() );

    GraphNode typeNode = graphModel.findNode( "Node5" );
    assertNotNull( typeNode );
    assertEquals( "Node5", typeNode.getName() );
    assertEquals( 1, typeNode.getLabels().size() );
    assertEquals( "Type", typeNode.getLabels().get( 0 ) );
    assertEquals( 1, typeNode.getProperties().size() );
    GraphProperty typeNodeNameProperty = typeNode.findProperty( "Prop10" );
    assertNotNull( typeNodeNameProperty );
    assertEquals( "Prop10", typeNodeNameProperty.getName() );
    assertEquals( "name", typeNodeNameProperty.getDescription() );
    assertEquals( "String", typeNodeNameProperty.getType().name() );
    assertEquals( true, typeNodeNameProperty.isPrimary() );
    GraphPresentation typeNodePresentation = typeNode.getPresentation();
    assertNotNull( typeNodePresentation );
    assertEquals( 1020, typeNodePresentation.getX() );
    assertEquals( 260, typeNodePresentation.getY() );

    GraphNode strengthNode = graphModel.findNode( "Node3" );
    assertNotNull( strengthNode );
    assertEquals( "Node3", strengthNode.getName() );
    assertEquals( 1, strengthNode.getLabels().size() );
    assertEquals( "Strength", strengthNode.getLabels().get( 0 ) );
    assertEquals( 1, strengthNode.getProperties().size() );
    GraphProperty strengthNodePctProperty = strengthNode.findProperty( "Prop8" );
    assertNotNull( strengthNodePctProperty );
    assertEquals( "Prop8", strengthNodePctProperty.getName() );
    assertEquals( "pct", strengthNodePctProperty.getDescription() );
    assertEquals( "String", strengthNodePctProperty.getType().name() );
    assertEquals( true, strengthNodePctProperty.isPrimary() );
    GraphPresentation strengthNodePresentation = strengthNode.getPresentation();
    assertNotNull( strengthNodePresentation );
    assertEquals( 1020, strengthNodePresentation.getX() );
    assertEquals( 440, strengthNodePresentation.getY() );

    GraphNode breweryNode = graphModel.findNode( "Node1" );
    assertNotNull( breweryNode );
    assertEquals( "Node1", breweryNode.getName() );
    assertEquals( 1, breweryNode.getLabels().size() );
    assertEquals( "Brewery", breweryNode.getLabels().get( 0 ) );
    assertEquals( 1, breweryNode.getProperties().size() );
    GraphProperty breweryNodeNameProperty = breweryNode.findProperty( "Prop7" );
    assertNotNull( breweryNodeNameProperty );
    assertEquals( "Prop7", breweryNodeNameProperty.getName() );
    assertEquals( "name", breweryNodeNameProperty.getDescription() );
    assertEquals( "String", breweryNodeNameProperty.getType().name() );
    assertEquals( true, breweryNodeNameProperty.isPrimary() );
    GraphPresentation breweryNodePresentation = breweryNode.getPresentation();
    assertNotNull( breweryNodePresentation );
    assertEquals( 1020, breweryNodePresentation.getX() );
    assertEquals( 620, breweryNodePresentation.getY() );

    GraphNode brandNode = graphModel.findNode( "Node0" );
    assertNotNull( brandNode );
    assertEquals( "Node0", brandNode.getName() );
    assertEquals( 1, brandNode.getLabels().size() );
    assertEquals( "Brand", brandNode.getLabels().get( 0 ) );
    assertEquals( 1, brandNode.getProperties().size() );
    GraphProperty brandNodeNameProperty = brandNode.findProperty( "Prop6" );
    assertNotNull( brandNodeNameProperty );
    assertEquals( "Prop6", brandNodeNameProperty.getName() );
    assertEquals( "name", brandNodeNameProperty.getDescription() );
    assertEquals( "String", brandNodeNameProperty.getType().name() );
    assertEquals( true, brandNodeNameProperty.isPrimary() );
    GraphPresentation brandNodePresentation = brandNode.getPresentation();
    assertNotNull( brandNodePresentation );
    assertEquals( 820, brandNodePresentation.getX() );
    assertEquals( 440, brandNodePresentation.getY() );

    // Validate the relationships
    //
    assertEquals( 3, graphModel.getRelationships().size() );
    GraphRelationship hasTypeRelationship = graphModel.findRelationship( "Rel6" );
    assertNotNull( hasTypeRelationship );
    assertEquals( "Node0", hasTypeRelationship.getNodeSource() );
    assertEquals( "Node5", hasTypeRelationship.getNodeTarget() );
    assertEquals( "has_type", hasTypeRelationship.getLabel() );

    GraphRelationship hasStrengthRelationship = graphModel.findRelationship( "Rel4" );
    assertNotNull( hasStrengthRelationship );
    assertEquals( "Node0", hasStrengthRelationship.getNodeSource() );
    assertEquals( "Node3", hasStrengthRelationship.getNodeTarget() );
    assertEquals( "has_strength", hasStrengthRelationship.getLabel() );

    GraphRelationship brewedByRelationship = graphModel.findRelationship( "Rel2" );
    assertNotNull( brewedByRelationship );
    assertEquals( "Node0", brewedByRelationship.getNodeSource() );
    assertEquals( "Node1", brewedByRelationship.getNodeTarget() );
    assertEquals( "brewed_by", brewedByRelationship.getLabel() );
  }

  @Test
  public void changeNamesToLabels() throws KettleException {

    GraphModel model = CypherWorkbenchImporter.changeNamesToLabels( graphModel );
    assertNotNull( model );

    assertEquals( "Not all nodes were imported", 4, model.getNodes().size() );
    assertEquals( "Not all relationships were imported", 3, model.getRelationships().size() );

    GraphNode typeNode = model.findNode( "Type" );
    assertNotNull( typeNode );
    assertEquals( "Type", typeNode.getName() );
    assertEquals( 1, typeNode.getLabels().size() );
    assertEquals( "Type", typeNode.getLabels().get( 0 ) );
    assertEquals( 1, typeNode.getProperties().size() );
    GraphProperty typeNodeNameProperty = typeNode.findProperty( "name" );
    assertNotNull( typeNodeNameProperty );
    assertEquals( "name", typeNodeNameProperty.getName() );
    assertNull( typeNodeNameProperty.getDescription() );
    assertEquals( "String", typeNodeNameProperty.getType().name() );
    assertEquals( true, typeNodeNameProperty.isPrimary() );
    GraphPresentation typeNodePresentation = typeNode.getPresentation();
    assertNotNull( typeNodePresentation );
    assertEquals( 1020, typeNodePresentation.getX() );
    assertEquals( 260, typeNodePresentation.getY() );

    GraphNode strengthNode = model.findNode( "Strength" );
    assertNotNull( strengthNode );
    assertEquals( "Strength", strengthNode.getName() );
    assertEquals( 1, strengthNode.getLabels().size() );
    assertEquals( "Strength", strengthNode.getLabels().get( 0 ) );
    assertEquals( 1, strengthNode.getProperties().size() );
    GraphProperty strengthNodePctProperty = strengthNode.findProperty( "pct" );
    assertNotNull( strengthNodePctProperty );
    assertEquals( "pct", strengthNodePctProperty.getName() );
    assertNull( strengthNodePctProperty.getDescription() );
    assertEquals( "String", strengthNodePctProperty.getType().name() );
    assertEquals( true, strengthNodePctProperty.isPrimary() );
    GraphPresentation strengthNodePresentation = strengthNode.getPresentation();
    assertNotNull( strengthNodePresentation );
    assertEquals( 1020, strengthNodePresentation.getX() );
    assertEquals( 440, strengthNodePresentation.getY() );

    GraphNode breweryNode = model.findNode( "Brewery" );
    assertNotNull( breweryNode );
    assertEquals( "Brewery", breweryNode.getName() );
    assertEquals( 1, breweryNode.getLabels().size() );
    assertEquals( "Brewery", breweryNode.getLabels().get( 0 ) );
    assertEquals( 1, breweryNode.getProperties().size() );
    GraphProperty breweryNodeNameProperty = breweryNode.findProperty( "name" );
    assertNotNull( breweryNodeNameProperty );
    assertEquals( "name", breweryNodeNameProperty.getName() );
    assertNull( breweryNodeNameProperty.getDescription() );
    assertEquals( "String", breweryNodeNameProperty.getType().name() );
    assertEquals( true, breweryNodeNameProperty.isPrimary() );
    GraphPresentation breweryNodePresentation = breweryNode.getPresentation();
    assertNotNull( breweryNodePresentation );
    assertEquals( 1020, breweryNodePresentation.getX() );
    assertEquals( 620, breweryNodePresentation.getY() );

    GraphNode brandNode = model.findNode( "Brand" );
    assertNotNull( brandNode );
    assertEquals( "Brand", brandNode.getName() );
    assertEquals( 1, brandNode.getLabels().size() );
    assertEquals( "Brand", brandNode.getLabels().get( 0 ) );
    assertEquals( 1, brandNode.getProperties().size() );
    GraphProperty brandNodeNameProperty = brandNode.findProperty( "name" );
    assertNotNull( brandNodeNameProperty );
    assertEquals( "name", brandNodeNameProperty.getName() );
    assertNull( brandNodeNameProperty.getDescription() );
    assertEquals( "String", brandNodeNameProperty.getType().name() );
    assertEquals( true, brandNodeNameProperty.isPrimary() );
    GraphPresentation brandNodePresentation = brandNode.getPresentation();
    assertNotNull( brandNodePresentation );
    assertEquals( 820, brandNodePresentation.getX() );
    assertEquals( 440, brandNodePresentation.getY() );

    // Validate the relationships
    //
    GraphRelationship hasTypeRelationship = model.findRelationship( "has_type" );
    assertNotNull( hasTypeRelationship );
    assertEquals( "Brand", hasTypeRelationship.getNodeSource() );
    assertEquals( "Type", hasTypeRelationship.getNodeTarget() );
    assertEquals( "Brand - Type", hasTypeRelationship.getDescription() );
    assertEquals( "has_type", hasTypeRelationship.getLabel() );

    GraphRelationship hasStrengthRelationship = model.findRelationship( "has_strength" );
    assertNotNull( hasStrengthRelationship );
    assertEquals( "Brand", hasStrengthRelationship.getNodeSource() );
    assertEquals( "Strength", hasStrengthRelationship.getNodeTarget() );
    assertEquals( "Brand - Strength", hasStrengthRelationship.getDescription() );
    assertEquals( "has_strength", hasStrengthRelationship.getLabel() );

    GraphRelationship brewedByRelationship = model.findRelationship( "brewed_by" );
    assertNotNull( brewedByRelationship );
    assertEquals( "Brand", brewedByRelationship.getNodeSource() );
    assertEquals( "Brewery", brewedByRelationship.getNodeTarget() );
    assertEquals( "Brand - Brewery", brewedByRelationship.getDescription() );
    assertEquals( "brewed_by", brewedByRelationship.getLabel() );
  }
}
