package bi.know.kettle.neo4j.core;

import org.apache.commons.lang.WordUtils;
import org.pentaho.di.core.row.ValueMetaInterface;

public class Neo4jUtil {

  private static final char[] delimitersLiteral = new char[] { ' ', '\t', ',', ';', '_', '-' };
  private static final String[] delimitersRegex = new String[] { "\\s", "\\t", ",", ";", "_", "-" };

  public static String standardizePropertyName( ValueMetaInterface valueMeta ) {
    String propertyName = valueMeta.getName();
    propertyName = WordUtils.capitalize( propertyName, delimitersLiteral );
    for ( String delimiterRegex : delimitersRegex ) {
      propertyName = propertyName.replaceAll( delimiterRegex, "" );
    }
    if ( propertyName.length() > 0 ) {
      propertyName = propertyName.substring( 0, 1 ).toLowerCase() + propertyName.substring( 1 );
    }
    return propertyName;
  }
}
