package org.neo4j.kettle.model.cw;

import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.neo4j.kettle.model.GraphModel;
import org.neo4j.kettle.model.GraphNode;
import org.neo4j.kettle.model.GraphPresentation;
import org.neo4j.kettle.model.GraphProperty;
import org.neo4j.kettle.model.GraphPropertyType;
import org.neo4j.kettle.model.GraphRelationship;
import org.pentaho.di.core.exception.KettleException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CypherWorkbenchImporter {

  public static GraphModel importFromCwJson( String jsonString ) throws KettleException {
    try {
      GraphModel graphModel = new GraphModel();
      JSONParser parser = new JSONParser();

      JSONObject jModel = (JSONObject) parser.parse( jsonString );
      JSONObject jMetadata = (JSONObject) jModel.get( "metadata" );

      String modelName = (String) jMetadata.get( "title" );
      graphModel.setName( modelName );
      String modelDescription = (String) jMetadata.get( "description" );
      graphModel.setDescription( modelDescription );

      JSONObject jDataModel = (JSONObject) jModel.get( "dataModel" );

      // Import the nodes
      //
      JSONObject jNodeLabels = (JSONObject) jDataModel.get( "nodeLabels" );
      for ( Object jNodeKey : jNodeLabels.keySet() ) {
        JSONObject jNodeLabel = (JSONObject) jNodeLabels.get( jNodeKey );
        String classTypeString = (String) jNodeLabel.get( "classType" );
        if ( "NodeLabel".equals( classTypeString ) ) {
          GraphNode graphNode = new GraphNode();
          String key = (String) jNodeLabel.get( "key" );
          String label = (String) jNodeLabel.get( "label" );
          graphNode.setName( key );
          graphNode.getLabels().add( label );

          // Get the properties
          //
          graphNode.getProperties().addAll( importProperties( jNodeLabel, "properties" ) );

          JSONObject jNodeDisplay = (JSONObject) jNodeLabel.get( "display" );
          Long nodeLocationX = (Long) jNodeDisplay.get( "x" );
          Long nodeLocationY = (Long) jNodeDisplay.get( "y" );
          if ( nodeLocationX != null && nodeLocationY != null ) {
            graphNode.setPresentation( new GraphPresentation( nodeLocationX.intValue(), nodeLocationY.intValue() ) );
          }
          graphModel.getNodes().add( graphNode );
        }
      }

      // Import the relationships...
      //
      JSONObject jRelationshipTypes = (JSONObject) jDataModel.get( "relationshipTypes" );
      for ( Object jRelationshipTypeKey : jRelationshipTypes.keySet() ) {
        JSONObject jRelationshipType = (JSONObject) jRelationshipTypes.get( jRelationshipTypeKey );
        String classTypeString = (String) jRelationshipType.get( "classType" );
        if ( "RelationshipType".equals( classTypeString ) ) {
          String relationshipKey = (String) jRelationshipType.get( "key" );
          String relationshipType = (String) jRelationshipType.get( "type" );
          String relationshipStartKey = (String) jRelationshipType.get( "startNodeLabelKey" );
          String relationshipEndKey = (String) jRelationshipType.get( "endNodeLabelKey" );
          List<GraphProperty> relationshipProperties = importProperties( jRelationshipType, "properties" );
          GraphRelationship graphRelationship = new GraphRelationship(
            relationshipKey,
            relationshipType,
            relationshipType,
            relationshipProperties,
            relationshipStartKey,
            relationshipEndKey
          );
          graphModel.getRelationships().add( graphRelationship );
        }
      }

      return graphModel;
    } catch ( Exception e ) {
      throw new KettleException( "Error parsing Cypher Workbench model", e );
    }
  }

  private static List<GraphProperty> importProperties( JSONObject j, String propertiesKey ) {
    List<GraphProperty> properties = new ArrayList<>();
    JSONObject jNodeProperties = (JSONObject) j.get( propertiesKey );
    if ( jNodeProperties != null ) {
      for ( Object jPropertyKey : jNodeProperties.keySet() ) {
        JSONObject jNodeProperty = (JSONObject) jNodeProperties.get( jPropertyKey );
        String propertyName = (String) jNodeProperty.get( "name" );
        String propertyKey = (String) jNodeProperty.get( "key" );
        String propertyTypeString = (String) jNodeProperty.get( "datatype" );
        Boolean propertyPartOfKey = (Boolean) jNodeProperty.get( "isPartOfKey" );
        GraphPropertyType propertyType = GraphPropertyType.parseCode( propertyTypeString );
        GraphProperty nodeProperty = new GraphProperty( propertyKey, propertyName, propertyType, propertyPartOfKey );
        properties.add( nodeProperty );
      }
    }
    return properties;
  }

  /**
   * Change imported keys to the provided labels.
   * Make sure we don't create any duplicates and so on.
   * This method does sanity checks on the labels and names.
   * This does not change the source graph model.
   *
   * @param sourceModel The source graph model, typically imported with importFromCwJson()
   * @return A modified copy of the source model
   */
  public static final GraphModel changeNamesToLabels( GraphModel sourceModel ) throws KettleException {
    GraphModel graphModel = new GraphModel( sourceModel );

    // Do sanity check on the labels
    //
    Set<String> nodeLabels = new HashSet<>();
    for ( GraphNode graphNode : graphModel.getNodes() ) {
      if ( graphNode.getLabels().isEmpty() ) {
        throw new KettleException( "No node labels found for node " + graphNode.getName() );
      }
      for ( String label : graphNode.getLabels() ) {
        if ( nodeLabels.contains( label ) ) {
          throw new KettleException( "Node label '" + label + "' is used more than once in model " + graphModel.getName() );
        }
      }
      // We also need to make sure that the label is not an existing node name...
      //
      String label = graphNode.getLabels().get( 0 );
      if ( graphModel.findNode( label ) != null ) {
        throw new KettleException( "A node named '" + label + "' already exists in the model. Renaming nodes might break consistency" );
      }

      nodeLabels.add( label );
    }

    // Now we can change the node name to the first label
    // Also change the node name in relationship source or target...
    //
    for ( GraphNode graphNode : graphModel.getNodes() ) {
      String label = graphNode.getLabels().get( 0 );
      String oldName = graphNode.getName();
      graphNode.setName( label );

      // Change the node properties : description --> name
      //
      for ( GraphProperty property : graphNode.getProperties() ) {
        property.setName( property.getDescription() );
        property.setDescription( null );
      }

      for ( GraphRelationship relationship : graphModel.getRelationships() ) {
        if ( relationship.getNodeSource().equals( oldName ) ) {
          relationship.setNodeSource( label );
        }
        if ( relationship.getNodeTarget().equals( oldName ) ) {
          relationship.setNodeTarget( label );
        }
        for ( GraphProperty property : relationship.getProperties() ) {
          property.setName( property.getDescription() );
          property.setDescription( null );
        }
      }
    }

    // Change the relationship names to their labels as well...
    // First do a sanity check on the relationship labels, check for duplicates
    //
    Set<String> relationshipLabels = new HashSet<>();
    for ( GraphRelationship relationship : graphModel.getRelationships() ) {
      String label = relationship.getLabel();
      if ( StringUtils.isEmpty( label ) ) {
        throw new KettleException( "No relationship label found for relationship between nodes: " + relationship.getNodeSource() + " and " + relationship.getNodeTarget() );
      }
      // We also need to make sure that the label is not an existing node name...
      //
      if ( graphModel.findRelationship( label ) != null ) {
        throw new KettleException( "A relationship named '" + label + "' already exists in the model. Renaming relationships might break consistency" );
      }

      relationshipLabels.add( label );
    }

    // Now rename the relationships to their labels
    // Also set a description
    //
    for ( GraphRelationship relationship : graphModel.getRelationships() ) {
      relationship.setName( relationship.getLabel() );
      relationship.setDescription( relationship.getNodeSource() + " - " + relationship.getNodeTarget() );
    }

    return graphModel;
  }
}
