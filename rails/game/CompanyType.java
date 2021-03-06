/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/CompanyType.java,v 1.11 2010/02/04 21:27:58 evos Exp $ */
package rails.game;

import java.util.ArrayList;
import java.util.List;

import rails.common.LocalText;
import rails.common.parser.ConfigurationException;
import rails.common.parser.Tag;

/**
 * Objects of this class represent a particular type of company, of which
 * typically multiple instances exist in a rails.game. Examples: "Private",
 * "Minor", "Major", "Mountain" etc. <p> This class contains common properties
 * of the companies of one type, and aids in configuring the companies by
 * reducing the need to repeatedly specify common properties with different
 * companies.
 */
public class CompanyType implements CompanyTypeI {

    /*--- Class attributes ---*/

    /*--- Instance attributes ---*/
    protected String name;
    protected String className;
    protected int capitalisation = PublicCompanyI.CAPITALISE_FULL;

    protected List<CompanyI> companies = new ArrayList<CompanyI>();

    /**
     * The constructor.
     *
     * @param name Company type name ("Private", "Public", "Minor" etc.).
     * @param className Name of the class that will instantiate this type of
     * company.
     * @param element The &lt;CompanyType&gt; DOM element, used to define this
     * company type.
     */
    public CompanyType(String name, String className) {
        this.name = name;
        this.className = className;
    }

    /**
     * @see rails.common.parser.ConfigurableComponentI#configureFromXML(org.w3c.dom.Element)
     */
    public void configureFromXML(Tag tag) throws ConfigurationException {
        //No longer needed.
    }

    public void finishConfiguration (GameManagerI gameManager) {

    }

    public CompanyI createCompany(String name, Tag typeTag, Tag tag)
    throws ConfigurationException {
        CompanyI newCompany = null;
        try {
            newCompany = (Company) Class.forName(className).newInstance();
        } catch (Exception e) {
            throw new ConfigurationException(LocalText.getText(
                    "ClassCannotBeInstantiated", className), e);
        }
        newCompany.init(name, this);
        newCompany.configureFromXML(typeTag);
        newCompany.configureFromXML(tag);
        companies.add(newCompany);
        return newCompany;
    }

    /*--- Getters and setters ---*/
    /**
     * Get the company type name
     *
     * @return The name of this company type.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the name of the class that will implement this type of company.
     *
     * @return The full class name.
     */
    public String getClassName() {
        return className;
    }

    public List<CompanyI> getCompanies() {
		return companies;
	}

	public void setCapitalisation(int mode) {
        this.capitalisation = mode;
    }

    public void setCapitalisation(String mode) {
        if (mode.equalsIgnoreCase("full")) {
            this.capitalisation = PublicCompanyI.CAPITALISE_FULL;
        } else if (mode.equalsIgnoreCase("incremental")) {
            this.capitalisation = PublicCompanyI.CAPITALISE_INCREMENTAL;
        }
    }

    public int getCapitalisation() {
        return capitalisation;
    }

}
