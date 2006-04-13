package org.apache.maven.plugin.pmd;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.xml.pull.MXParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Base class for mojos that check if there were any PMD violations.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public abstract class AbstractPmdViolationCheckMojo
    extends AbstractMojo
{
    /**
     * The location of the XML report to check, as generated by the PMD report.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private File targetDirectory;

    /**
     * Whether to fail the build if the validation check fails.
     *
     * @parameter expression="${failOnViolation}" default-value="true"
     * @required
     */
    private boolean failOnViolation;

    /**
     * The project language, for determining whether to run the report.
     *
     * @parameter expression="${project.artifact.artifactHandler.language}"
     * @required
     * @readonly
     */
    private String language;

    /**
     * The project source directory.
     *
     * @parameter expression="${project.build.sourceDirectory}"
     * @required
     * @readonly
     */
    private File sourceDirectory;

    protected void executeCheck( String filename, String tagName, String key )
        throws MojoFailureException, MojoExecutionException
    {
        if ( "java".equals( language ) && sourceDirectory.exists() )
        {
            File outputFile = new File( targetDirectory, filename );
            if ( outputFile.exists() )
            {
                try
                {
                    XmlPullParser xpp = new MXParser();
                    FileReader freader = new FileReader( outputFile );
                    BufferedReader breader = new BufferedReader( freader );
                    xpp.setInput( breader );

                    int violations = countViolations( xpp, tagName );
                    if ( violations > 0 && failOnViolation )
                    {
                        throw new MojoFailureException(
                            "You have " + violations + " " + key + ( violations > 1 ? "s" : "" ) + "." );
                    }
                }
                catch ( IOException e )
                {
                    throw new MojoExecutionException( "Unable to read PMD results xml: " + outputFile.getAbsolutePath(),
                                                      e );
                }
                catch ( XmlPullParserException e )
                {
                    throw new MojoExecutionException( "Unable to read PMD results xml: " + outputFile.getAbsolutePath(),
                                                      e );
                }
            }
            else
            {
                throw new MojoFailureException( "Unable to perform check, " + "unable to find " + outputFile );
            }
        }
    }

    private int countViolations( XmlPullParser xpp, String tagName )
        throws XmlPullParserException, IOException
    {
        int count = 0;

        int eventType = xpp.getEventType();
        while ( eventType != XmlPullParser.END_DOCUMENT )
        {
            if ( eventType == XmlPullParser.START_TAG && tagName.equals( xpp.getName() ) )
            {
                count++;
            }
            eventType = xpp.next();
        }

        return count;
    }
}
