package com.fakedata.content;

import net.datafaker.Faker;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Provides themed, realistic content for generated documents using Datafaker.
 */
public class ContentProvider {
    private final Faker faker;
    private final Random random;
    private final Theme theme;

    /**
     * Available content themes.
     */
    public enum Theme {
        FINANCIAL,
        ENTERTAINMENT,
        HEALTHCARE,
        TECHNOLOGY,
        LEGAL,
        EDUCATION,
        RETAIL,
        DEFAULT
    }

    // Theme-specific document names
    private static final Map<Theme, String[]> PDF_NAMES_BY_THEME = Map.of(
            Theme.FINANCIAL, new String[]{"Quarterly_Financial_Report", "Annual_Audit_Report", "Investment_Analysis", "Tax_Filing_Summary", "Portfolio_Review", "Risk_Assessment", "Compliance_Report", "Budget_Analysis", "Cash_Flow_Statement", "Shareholder_Report"},
            Theme.ENTERTAINMENT, new String[]{"Production_Schedule", "Talent_Contract_Summary", "Box_Office_Report", "Audience_Analytics", "Content_Strategy", "Media_Rights_Overview", "Festival_Submission", "Marketing_Campaign_Review", "Streaming_Performance", "Ratings_Analysis"},
            Theme.HEALTHCARE, new String[]{"Patient_Care_Report", "Clinical_Trial_Summary", "Compliance_Audit", "Medical_Research_Findings", "Treatment_Protocol", "Health_Outcomes_Report", "Regulatory_Filing", "Quality_Assurance_Report", "Safety_Analysis", "Population_Health_Study"},
            Theme.TECHNOLOGY, new String[]{"Technical_Architecture", "System_Design_Document", "Security_Audit", "Performance_Analysis", "API_Documentation", "Infrastructure_Review", "DevOps_Report", "Code_Quality_Assessment", "Cloud_Migration_Plan", "Data_Pipeline_Overview"},
            Theme.LEGAL, new String[]{"Case_Summary", "Contract_Review", "Legal_Opinion", "Compliance_Assessment", "Litigation_Report", "Due_Diligence_Findings", "Regulatory_Analysis", "Policy_Review", "Risk_Assessment", "Settlement_Summary"},
            Theme.EDUCATION, new String[]{"Curriculum_Review", "Student_Performance_Report", "Accreditation_Document", "Research_Grant_Proposal", "Faculty_Assessment", "Learning_Outcomes_Analysis", "Enrollment_Statistics", "Academic_Program_Review", "Campus_Planning_Report", "Budget_Allocation_Summary"},
            Theme.RETAIL, new String[]{"Sales_Performance_Report", "Inventory_Analysis", "Customer_Insights", "Market_Trends_Report", "Store_Operations_Review", "Supply_Chain_Analysis", "Seasonal_Forecast", "Merchandising_Strategy", "E-commerce_Analytics", "Loss_Prevention_Report"},
            Theme.DEFAULT, new String[]{"Quarterly_Financial_Report", "Annual_Business_Review", "Executive_Summary", "Market_Analysis", "Strategic_Plan", "Compliance_Report", "Risk_Assessment", "Performance_Review", "Project_Status_Report", "Audit_Findings"}
    );

    private static final Map<Theme, String[]> SPREADSHEET_NAMES_BY_THEME = Map.of(
            Theme.FINANCIAL, new String[]{"Budget_Projections", "Financial_Statements", "Investment_Portfolio", "Tax_Calculations", "Revenue_Forecast", "Expense_Tracking", "Cash_Flow_Model", "Loan_Amortization", "Asset_Valuation", "ROI_Analysis"},
            Theme.ENTERTAINMENT, new String[]{"Production_Budget", "Royalty_Calculations", "Ticket_Sales_Data", "Content_Schedule", "Revenue_Share_Model", "Cast_Crew_Budget", "Marketing_Spend", "Distribution_Revenue", "Merchandise_Sales", "Event_Attendance"},
            Theme.HEALTHCARE, new String[]{"Patient_Statistics", "Treatment_Costs", "Resource_Allocation", "Clinical_Data", "Insurance_Claims", "Staff_Scheduling", "Equipment_Inventory", "Medication_Tracking", "Outcome_Metrics", "Budget_Allocation"},
            Theme.TECHNOLOGY, new String[]{"Sprint_Velocity", "Bug_Tracking", "Server_Metrics", "Cost_Analysis", "User_Analytics", "Feature_Backlog", "Release_Schedule", "Resource_Planning", "License_Inventory", "Performance_Benchmarks"},
            Theme.LEGAL, new String[]{"Billable_Hours", "Case_Tracking", "Settlement_Calculations", "Client_Billing", "Document_Index", "Deadline_Tracker", "Fee_Schedule", "Matter_Budget", "Conflict_Check", "Discovery_Log"},
            Theme.EDUCATION, new String[]{"Grade_Book", "Enrollment_Data", "Course_Schedule", "Budget_Allocation", "Research_Funding", "Student_Demographics", "Faculty_Load", "Tuition_Revenue", "Scholarship_Awards", "Resource_Utilization"},
            Theme.RETAIL, new String[]{"Sales_Data", "Inventory_Levels", "Pricing_Analysis", "Customer_Database", "Vendor_Costs", "Profit_Margins", "Store_Performance", "Seasonal_Trends", "Promotion_Results", "Supply_Chain_Costs"},
            Theme.DEFAULT, new String[]{"Budget_Projections", "Financial_Statements", "Sales_Forecast", "Expense_Tracker", "Revenue_Analysis", "Cost_Breakdown", "Inventory_Report", "Payroll_Summary", "Cash_Flow_Statement", "KPI_Dashboard"}
    );

    private static final Map<Theme, String[]> DOCUMENT_NAMES_BY_THEME = Map.of(
            Theme.FINANCIAL, new String[]{"Investment_Memo", "Policy_Guidelines", "Account_Agreement", "Regulatory_Filing", "Audit_Procedures", "Risk_Management_Policy", "Compliance_Manual", "Client_Onboarding", "Due_Diligence_Report", "Financial_Procedures"},
            Theme.ENTERTAINMENT, new String[]{"Script_Draft", "Production_Notes", "Talent_Agreement", "Distribution_Contract", "Creative_Brief", "Press_Release", "Show_Bible", "Location_Agreement", "Music_License", "Publicity_Materials"},
            Theme.HEALTHCARE, new String[]{"Treatment_Protocol", "Patient_Consent", "Clinical_Guidelines", "Research_Protocol", "HIPAA_Compliance", "Care_Plan", "Medical_Records_Policy", "Incident_Report", "Quality_Standards", "Staff_Procedures"},
            Theme.TECHNOLOGY, new String[]{"Technical_Specification", "User_Guide", "API_Reference", "Security_Policy", "Development_Standards", "Operations_Manual", "Incident_Response_Plan", "Data_Governance_Policy", "Architecture_Decision_Record", "Runbook"},
            Theme.LEGAL, new String[]{"Contract_Draft", "Legal_Brief", "Motion_Filing", "Discovery_Request", "Settlement_Agreement", "Client_Engagement_Letter", "Legal_Memorandum", "Court_Filing", "Witness_Statement", "Deposition_Summary"},
            Theme.EDUCATION, new String[]{"Syllabus", "Course_Materials", "Research_Paper", "Grant_Application", "Accreditation_Report", "Student_Handbook", "Faculty_Guidelines", "Thesis_Draft", "Curriculum_Guide", "Assessment_Rubric"},
            Theme.RETAIL, new String[]{"Operations_Manual", "Employee_Handbook", "Vendor_Agreement", "Return_Policy", "Customer_Service_Guide", "Merchandising_Guidelines", "Store_Procedures", "Training_Materials", "Brand_Guidelines", "Promotion_Terms"},
            Theme.DEFAULT, new String[]{"Meeting_Minutes", "Project_Proposal", "Policy_Document", "Implementation_Guide", "Technical_Specification", "Business_Requirements", "Contract_Draft", "Terms_and_Conditions", "User_Manual", "Process_Documentation"}
    );

    private static final Map<Theme, String[]> PRESENTATION_NAMES_BY_THEME = Map.of(
            Theme.FINANCIAL, new String[]{"Investor_Presentation", "Quarterly_Earnings", "Fund_Overview", "Market_Outlook", "Portfolio_Review", "IPO_Roadshow", "Board_Update", "Risk_Committee_Briefing", "Strategy_Review", "Client_Pitch"},
            Theme.ENTERTAINMENT, new String[]{"Pitch_Deck", "Production_Kickoff", "Marketing_Campaign", "Premiere_Presentation", "Network_Upfront", "Festival_Pitch", "Talent_Showcase", "Distribution_Proposal", "Brand_Partnership", "Content_Strategy"},
            Theme.HEALTHCARE, new String[]{"Clinical_Presentation", "Research_Findings", "Treatment_Overview", "Patient_Education", "Staff_Training", "Regulatory_Update", "Quality_Review", "Safety_Briefing", "Department_Update", "Board_Presentation"},
            Theme.TECHNOLOGY, new String[]{"Product_Launch", "Technical_Overview", "Architecture_Review", "Sprint_Demo", "Roadmap_Presentation", "Security_Briefing", "Cloud_Strategy", "Innovation_Showcase", "Partner_Pitch", "Team_Onboarding"},
            Theme.LEGAL, new String[]{"Case_Presentation", "Client_Briefing", "Trial_Preparation", "Settlement_Proposal", "Compliance_Training", "Firm_Overview", "Practice_Area_Update", "CLE_Presentation", "Expert_Testimony", "Mediation_Overview"},
            Theme.EDUCATION, new String[]{"Course_Introduction", "Research_Presentation", "Department_Overview", "Accreditation_Review", "Faculty_Meeting", "Student_Orientation", "Graduation_Ceremony", "Alumni_Update", "Fundraising_Pitch", "Board_Presentation"},
            Theme.RETAIL, new String[]{"Sales_Kickoff", "Product_Launch", "Store_Manager_Meeting", "Vendor_Presentation", "Marketing_Strategy", "Holiday_Planning", "Training_Session", "Franchise_Overview", "Customer_Insights", "Quarterly_Review"},
            Theme.DEFAULT, new String[]{"Board_Presentation", "Investor_Pitch", "Product_Launch", "Training_Materials", "Company_Overview", "Strategy_Presentation", "Sales_Pitch", "Quarterly_Update", "Project_Kickoff", "Team_Introduction"}
    );

    private static final Map<Theme, String[]> IMAGE_NAMES_BY_THEME = Map.of(
            Theme.FINANCIAL, new String[]{"Portfolio_Allocation_Chart", "Market_Performance_Graph", "Revenue_Breakdown", "Investment_Returns", "Risk_Heat_Map", "Cash_Flow_Diagram", "Asset_Distribution", "Growth_Projection", "Expense_Pie_Chart", "Trading_Volume"},
            Theme.ENTERTAINMENT, new String[]{"Ratings_Chart", "Box_Office_Graph", "Audience_Demographics", "Content_Calendar", "Social_Media_Metrics", "Streaming_Analytics", "Production_Timeline", "Revenue_Distribution", "Engagement_Metrics", "Market_Share"},
            Theme.HEALTHCARE, new String[]{"Patient_Outcomes_Chart", "Treatment_Effectiveness", "Resource_Utilization", "Clinical_Trial_Results", "Population_Health_Map", "Quality_Metrics", "Safety_Dashboard", "Staffing_Chart", "Cost_Analysis", "Procedure_Statistics"},
            Theme.TECHNOLOGY, new String[]{"System_Architecture_Diagram", "Performance_Metrics", "User_Growth_Chart", "Infrastructure_Map", "Sprint_Burndown", "Code_Coverage", "API_Traffic", "Error_Rate_Graph", "Deployment_Pipeline", "Resource_Usage"},
            Theme.LEGAL, new String[]{"Case_Timeline", "Settlement_Breakdown", "Billing_Summary", "Matter_Statistics", "Practice_Area_Revenue", "Client_Portfolio", "Win_Rate_Chart", "Hours_Distribution", "Fee_Analysis", "Workflow_Diagram"},
            Theme.EDUCATION, new String[]{"Enrollment_Trends", "Grade_Distribution", "Research_Output", "Funding_Allocation", "Student_Demographics", "Graduation_Rates", "Faculty_Composition", "Course_Evaluation", "Campus_Map", "Budget_Breakdown"},
            Theme.RETAIL, new String[]{"Sales_Trend_Chart", "Inventory_Levels", "Customer_Journey_Map", "Store_Performance", "Market_Share_Pie", "Seasonal_Comparison", "Promotion_ROI", "Supply_Chain_Flow", "Customer_Segments", "Revenue_by_Category"},
            Theme.DEFAULT, new String[]{"Organization_Chart", "Process_Flowchart", "Sales_Chart", "Revenue_Graph", "Market_Share_Diagram", "Timeline_Graphic", "Infographic", "Department_Structure", "Workflow_Diagram", "Performance_Chart"}
    );

    private static final Map<Theme, String[]> COMPANY_PREFIXES_BY_THEME = Map.of(
            Theme.FINANCIAL, new String[]{"Capital", "Wealth", "Asset", "Investment", "Financial", "Trust", "Securities", "Advisory", "Partners", "Holdings"},
            Theme.ENTERTAINMENT, new String[]{"Creative", "Media", "Studios", "Productions", "Entertainment", "Digital", "Content", "Pictures", "Films", "Network"},
            Theme.HEALTHCARE, new String[]{"Medical", "Health", "Care", "Clinical", "Wellness", "Bio", "Life", "Pharma", "Therapeutics", "Diagnostics"},
            Theme.TECHNOLOGY, new String[]{"Tech", "Digital", "Cloud", "Data", "Software", "Systems", "Solutions", "Labs", "Innovations", "Computing"},
            Theme.LEGAL, new String[]{"Law", "Legal", "Counsel", "Attorneys", "Associates", "Partners", "Advocates", "Barristers", "Solicitors", "Chambers"},
            Theme.EDUCATION, new String[]{"Academy", "Institute", "University", "College", "School", "Learning", "Education", "Research", "Foundation", "Center"},
            Theme.RETAIL, new String[]{"Retail", "Store", "Shop", "Market", "Goods", "Commerce", "Trading", "Merchants", "Outlet", "Emporium"},
            Theme.DEFAULT, new String[]{"Global", "Premier", "Elite", "United", "National", "International", "Strategic", "Dynamic", "Innovative", "Advanced"}
    );

    private static final Map<Theme, String[]> DEPARTMENTS_BY_THEME = Map.of(
            Theme.FINANCIAL, new String[]{"Trading", "Risk Management", "Compliance", "Portfolio Management", "Client Services", "Research", "Operations", "Treasury"},
            Theme.ENTERTAINMENT, new String[]{"Production", "Creative Development", "Distribution", "Marketing", "Talent Relations", "Legal Affairs", "Business Affairs", "Post-Production"},
            Theme.HEALTHCARE, new String[]{"Patient Care", "Clinical Research", "Quality Assurance", "Nursing", "Pharmacy", "Radiology", "Laboratory", "Administration"},
            Theme.TECHNOLOGY, new String[]{"Engineering", "Product", "DevOps", "Security", "Data Science", "QA", "Infrastructure", "Customer Success"},
            Theme.LEGAL, new String[]{"Litigation", "Corporate", "Real Estate", "Tax", "Employment", "IP", "Regulatory", "Pro Bono"},
            Theme.EDUCATION, new String[]{"Academic Affairs", "Student Services", "Research", "Admissions", "Financial Aid", "IT Services", "Facilities", "Alumni Relations"},
            Theme.RETAIL, new String[]{"Store Operations", "Merchandising", "Supply Chain", "E-commerce", "Customer Service", "Marketing", "Loss Prevention", "HR"},
            Theme.DEFAULT, new String[]{"Finance", "Marketing", "Sales", "Operations", "HR", "IT", "Legal", "R&D"}
    );

    public ContentProvider() {
        this(Theme.DEFAULT);
    }

    public ContentProvider(Theme theme) {
        this.faker = new Faker();
        this.random = new Random();
        this.theme = theme != null ? theme : Theme.DEFAULT;
    }

    public ContentProvider(long seed) {
        this(Theme.DEFAULT, seed);
    }

    public ContentProvider(Theme theme, long seed) {
        this.faker = new Faker(new Random(seed));
        this.random = new Random(seed);
        this.theme = theme != null ? theme : Theme.DEFAULT;
    }

    /**
     * Parses a theme string to a Theme enum.
     *
     * @param themeStr the theme string (case-insensitive)
     * @return the corresponding Theme, or DEFAULT if not recognized
     */
    public static Theme parseTheme(String themeStr) {
        if (themeStr == null || themeStr.isBlank()) {
            return Theme.DEFAULT;
        }
        String normalized = themeStr.trim().toUpperCase()
                .replace(" ", "_")
                .replace("&", "_")
                .replace("-", "_");

        // Handle common aliases
        if (normalized.contains("MEDIA") || normalized.contains("ENTERTAINMENT")) {
            return Theme.ENTERTAINMENT;
        }
        if (normalized.contains("FINANCE") || normalized.contains("FINANCIAL") || normalized.contains("BANKING")) {
            return Theme.FINANCIAL;
        }
        if (normalized.contains("HEALTH") || normalized.contains("MEDICAL") || normalized.contains("PHARMA")) {
            return Theme.HEALTHCARE;
        }
        if (normalized.contains("TECH") || normalized.contains("SOFTWARE") || normalized.contains("IT")) {
            return Theme.TECHNOLOGY;
        }
        if (normalized.contains("LAW") || normalized.contains("LEGAL")) {
            return Theme.LEGAL;
        }
        if (normalized.contains("EDU") || normalized.contains("SCHOOL") || normalized.contains("UNIVERSITY")) {
            return Theme.EDUCATION;
        }
        if (normalized.contains("RETAIL") || normalized.contains("STORE") || normalized.contains("SHOP")) {
            return Theme.RETAIL;
        }

        try {
            return Theme.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return Theme.DEFAULT;
        }
    }

    public Theme getTheme() {
        return theme;
    }

    // Document naming methods
    public String getPdfName() {
        String[] names = PDF_NAMES_BY_THEME.getOrDefault(theme, PDF_NAMES_BY_THEME.get(Theme.DEFAULT));
        return names[random.nextInt(names.length)];
    }

    public String getSpreadsheetName() {
        String[] names = SPREADSHEET_NAMES_BY_THEME.getOrDefault(theme, SPREADSHEET_NAMES_BY_THEME.get(Theme.DEFAULT));
        return names[random.nextInt(names.length)];
    }

    public String getDocumentName() {
        String[] names = DOCUMENT_NAMES_BY_THEME.getOrDefault(theme, DOCUMENT_NAMES_BY_THEME.get(Theme.DEFAULT));
        return names[random.nextInt(names.length)];
    }

    public String getPresentationName() {
        String[] names = PRESENTATION_NAMES_BY_THEME.getOrDefault(theme, PRESENTATION_NAMES_BY_THEME.get(Theme.DEFAULT));
        return names[random.nextInt(names.length)];
    }

    public String getImageName() {
        String[] names = IMAGE_NAMES_BY_THEME.getOrDefault(theme, IMAGE_NAMES_BY_THEME.get(Theme.DEFAULT));
        return names[random.nextInt(names.length)];
    }

    // Business content methods
    public String getCompanyName() {
        String[] prefixes = COMPANY_PREFIXES_BY_THEME.getOrDefault(theme, COMPANY_PREFIXES_BY_THEME.get(Theme.DEFAULT));
        String prefix = prefixes[random.nextInt(prefixes.length)];
        return faker.company().name().split(" ")[0] + " " + prefix;
    }

    public String getBusinessBuzzword() {
        return faker.company().buzzword();
    }

    public String getIndustry() {
        return switch (theme) {
            case FINANCIAL -> "Financial Services";
            case ENTERTAINMENT -> "Media & Entertainment";
            case HEALTHCARE -> "Healthcare";
            case TECHNOLOGY -> "Technology";
            case LEGAL -> "Legal Services";
            case EDUCATION -> "Education";
            case RETAIL -> "Retail";
            default -> faker.company().industry();
        };
    }

    public String getDepartment() {
        String[] departments = DEPARTMENTS_BY_THEME.getOrDefault(theme, DEPARTMENTS_BY_THEME.get(Theme.DEFAULT));
        return departments[random.nextInt(departments.length)];
    }

    public double getRandomDouble() {
        return random.nextDouble();
    }

    public String getJobTitle() {
        return faker.job().title();
    }

    public String getFullName() {
        return faker.name().fullName();
    }

    public String getEmail() {
        return faker.internet().emailAddress();
    }

    public String getPhoneNumber() {
        return faker.phoneNumber().phoneNumber();
    }

    public String getAddress() {
        return faker.address().fullAddress();
    }

    // Financial content methods
    public double getAmount(double min, double max) {
        return min + (random.nextDouble() * (max - min));
    }

    public int getPercentage() {
        return random.nextInt(101);
    }

    public int getYear() {
        return LocalDate.now().getYear() + random.nextInt(3) - 1;
    }

    public String getQuarter() {
        return "Q" + (random.nextInt(4) + 1);
    }

    public String getMonth() {
        String[] months = {"January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};
        return months[random.nextInt(months.length)];
    }

    // Business sentence templates for meaningful English content
    private static final Map<Theme, String[]> SENTENCES_BY_THEME = Map.of(
            Theme.FINANCIAL, new String[]{
                    "Our portfolio demonstrated strong resilience despite market volatility.",
                    "Client assets under management increased by double digits this quarter.",
                    "Risk-adjusted returns exceeded benchmark performance across all strategies.",
                    "Regulatory compliance remains a top priority with zero audit findings.",
                    "New investment products attracted significant institutional interest.",
                    "Trading volumes reached record levels in the derivatives segment.",
                    "Cost optimization initiatives delivered substantial operational savings.",
                    "Digital transformation efforts improved client onboarding efficiency.",
                    "Cross-selling opportunities drove growth in fee-based revenue.",
                    "Market share gains were achieved in key geographic regions."
            },
            Theme.ENTERTAINMENT, new String[]{
                    "Audience engagement metrics exceeded projections across all platforms.",
                    "Original content investments are generating strong subscriber growth.",
                    "Strategic partnerships expanded our global distribution reach.",
                    "Award recognition enhanced brand prestige and talent attraction.",
                    "Live event attendance set new records in major markets.",
                    "Streaming platform performance improved viewer retention rates.",
                    "Content licensing deals secured valuable intellectual property revenue.",
                    "Production efficiency gains reduced time-to-market for new releases.",
                    "Social media campaigns achieved viral reach and engagement.",
                    "Merchandise sales benefited from successful franchise expansion."
            },
            Theme.HEALTHCARE, new String[]{
                    "Patient satisfaction scores improved significantly this reporting period.",
                    "Clinical outcomes data demonstrates continued excellence in care quality.",
                    "New treatment protocols reduced average length of stay.",
                    "Telehealth adoption expanded access to underserved populations.",
                    "Research initiatives secured substantial grant funding for clinical trials.",
                    "Staff retention improved through enhanced professional development programs.",
                    "Quality metrics exceeded regulatory requirements across all departments.",
                    "Technology investments streamlined clinical workflow efficiency.",
                    "Community health programs reached more participants than projected.",
                    "Patient safety initiatives resulted in measurable harm reduction."
            },
            Theme.TECHNOLOGY, new String[]{
                    "Platform reliability achieved industry-leading uptime performance.",
                    "User adoption metrics exceeded growth targets for the quarter.",
                    "Security enhancements strengthened our defensive posture significantly.",
                    "Cloud migration delivered substantial infrastructure cost savings.",
                    "API integration capabilities expanded our partner ecosystem.",
                    "Machine learning implementations improved product recommendations.",
                    "Mobile application downloads surpassed significant milestones.",
                    "Development velocity increased through improved automation tooling.",
                    "Customer success initiatives reduced churn to historic lows.",
                    "Technical debt reduction improved system maintainability."
            },
            Theme.LEGAL, new String[]{
                    "Case outcomes achieved favorable results for our clients.",
                    "Practice area expertise expanded through strategic lateral hires.",
                    "Client satisfaction surveys reflected strong relationship management.",
                    "Pro bono contributions exceeded our annual commitment targets.",
                    "Technology investments improved document review efficiency.",
                    "Business development efforts generated significant new matters.",
                    "Professional development programs enhanced associate retention.",
                    "Cross-practice collaboration delivered comprehensive client solutions.",
                    "Thought leadership publications enhanced market visibility.",
                    "Regulatory expertise positioned the firm for emerging opportunities."
            },
            Theme.EDUCATION, new String[]{
                    "Student achievement metrics demonstrated continued academic excellence.",
                    "Research output increased substantially with notable publications.",
                    "Enrollment growth reflected strong demand for our programs.",
                    "Graduate employment outcomes exceeded national benchmarks.",
                    "Alumni engagement initiatives strengthened donor relationships.",
                    "Faculty recruitment attracted distinguished scholars to campus.",
                    "Online learning expansion reached new student populations.",
                    "Campus infrastructure investments enhanced the learning environment.",
                    "Scholarship funding increased access for deserving students.",
                    "Accreditation reviews confirmed institutional quality standards."
            },
            Theme.RETAIL, new String[]{
                    "Same-store sales growth outperformed industry averages.",
                    "E-commerce revenue expanded as digital capabilities improved.",
                    "Customer loyalty program membership reached new highs.",
                    "Inventory management optimization reduced carrying costs.",
                    "Store expansion strategy delivered strong unit economics.",
                    "Supply chain improvements shortened delivery lead times.",
                    "Private label products gained market share in key categories.",
                    "Customer service initiatives improved satisfaction ratings.",
                    "Promotional campaigns drove traffic and conversion improvements.",
                    "Sustainability initiatives resonated with environmentally conscious consumers."
            },
            Theme.DEFAULT, new String[]{
                    "Strategic initiatives delivered results aligned with organizational objectives.",
                    "Operational efficiency improvements generated meaningful cost savings.",
                    "Market position strengthened through focused competitive differentiation.",
                    "Team performance exceeded expectations across key metrics.",
                    "Customer relationships deepened through enhanced service delivery.",
                    "Innovation investments positioned the organization for future growth.",
                    "Risk management practices maintained strong governance standards.",
                    "Talent development programs built critical organizational capabilities.",
                    "Process improvements streamlined operations and reduced cycle times.",
                    "Stakeholder communication enhanced transparency and trust."
            }
    );

    private static final Map<Theme, String[]> PARAGRAPHS_BY_THEME = Map.of(
            Theme.FINANCIAL, new String[]{
                    "The investment portfolio demonstrated exceptional performance during this period, with risk-adjusted returns exceeding benchmark indices. Our diversified approach to asset allocation proved effective in navigating market volatility while capturing upside opportunities. Client retention remained strong as relationship managers delivered personalized service and strategic guidance.",
                    "Market conditions presented both challenges and opportunities for our trading operations. The team successfully identified arbitrage opportunities while maintaining disciplined risk management protocols. Technology investments in algorithmic trading capabilities enhanced execution quality and reduced transaction costs for institutional clients.",
                    "Regulatory compliance initiatives continued to strengthen our operational framework. Internal audit reviews confirmed adherence to all applicable requirements, and staff training programs ensured awareness of evolving regulatory expectations. These efforts position us well for the increasingly complex compliance landscape."
            },
            Theme.ENTERTAINMENT, new String[]{
                    "Content production activities accelerated during this period, with multiple projects advancing through development and into production phases. Creative teams collaborated effectively to develop compelling narratives that resonate with target audiences. Quality standards remained high while production efficiency improved through optimized workflows.",
                    "Audience engagement across our platforms exceeded expectations, driven by strategic content releases and effective marketing campaigns. Social media presence expanded significantly, generating organic reach and community building. Analytics-driven programming decisions improved content performance and viewer satisfaction metrics.",
                    "Distribution partnerships expanded our global footprint, opening new markets and revenue streams. Licensing negotiations secured favorable terms for our content library, while streaming platform performance demonstrated the enduring value of our intellectual property portfolio."
            },
            Theme.HEALTHCARE, new String[]{
                    "Clinical excellence remained our highest priority, with patient outcomes demonstrating the effectiveness of our care protocols. Quality improvement initiatives targeted specific areas for enhancement, resulting in measurable improvements across key performance indicators. Staff dedication to patient-centered care continued to drive positive experiences.",
                    "Research activities advanced significantly, with several studies progressing through critical milestones. Grant applications secured funding for innovative investigations into treatment approaches. Collaboration with academic partners enriched our research capabilities and contributed to the broader medical knowledge base.",
                    "Operational improvements enhanced our ability to serve patients efficiently while maintaining the highest standards of care. Technology implementations streamlined clinical workflows, allowing healthcare providers to focus more time on direct patient interaction. These investments support our commitment to accessible, high-quality healthcare."
            },
            Theme.TECHNOLOGY, new String[]{
                    "Platform development progressed according to roadmap priorities, with key features delivered on schedule. Engineering teams implemented architectural improvements that enhanced system scalability and reliability. User feedback informed iterative enhancements that improved the overall product experience.",
                    "Security initiatives strengthened our defensive capabilities through implementation of advanced threat detection and response mechanisms. Vulnerability assessments identified areas for improvement, and remediation efforts addressed findings promptly. These measures reflect our commitment to protecting customer data and maintaining system integrity.",
                    "Cloud infrastructure optimization delivered significant performance improvements while reducing operational costs. Migration of legacy systems to modern architectures enhanced development velocity and deployment flexibility. These technical investments establish a foundation for continued innovation and growth."
            },
            Theme.LEGAL, new String[]{
                    "Legal practice activities produced favorable outcomes for clients across diverse matters. Litigation teams achieved successful resolutions through strategic advocacy and thorough preparation. Transactional practice groups closed significant deals that advanced client business objectives.",
                    "Professional development investments enhanced capabilities across the firm. Training programs addressed emerging legal topics and practice skills. Mentorship initiatives supported associate growth and contributed to a culture of continuous learning and excellence.",
                    "Client service remained central to our practice philosophy. Relationship partners maintained regular communication to understand evolving client needs. Proactive legal counsel helped clients navigate complex regulatory environments and business challenges effectively."
            },
            Theme.EDUCATION, new String[]{
                    "Academic programs continued to attract talented students seeking quality education. Curriculum enhancements ensured relevance to contemporary career requirements while maintaining rigorous intellectual standards. Faculty members demonstrated commitment to both teaching excellence and scholarly contributions.",
                    "Research productivity increased substantially, with faculty publications appearing in leading peer-reviewed journals. Grant funding supported innovative investigations across disciplines. Graduate students contributed meaningfully to research projects while developing professional competencies.",
                    "Campus life enriched the educational experience through diverse programming and student activities. Support services addressed student needs holistically, contributing to retention and success. Alumni connections provided networking opportunities and career guidance for current students."
            },
            Theme.RETAIL, new String[]{
                    "Retail operations delivered strong results through effective execution of merchandising strategies. Store teams provided excellent customer service that drove loyalty and repeat purchases. Inventory management improvements ensured product availability while optimizing working capital utilization.",
                    "Digital commerce capabilities expanded to meet evolving customer expectations. Mobile shopping experiences improved through user interface enhancements and streamlined checkout processes. Omnichannel integration allowed customers to engage seamlessly across shopping channels.",
                    "Supply chain performance supported business growth through reliable product sourcing and distribution. Vendor partnerships strengthened through collaborative planning and performance management. Logistics network optimization reduced delivery times while improving cost efficiency."
            },
            Theme.DEFAULT, new String[]{
                    "Organizational performance reflected effective execution of strategic priorities. Teams collaborated across functions to deliver integrated solutions that addressed stakeholder needs. Leadership provided clear direction while empowering individuals to contribute their best efforts.",
                    "Continuous improvement initiatives enhanced operational effectiveness across the organization. Process analysis identified opportunities for streamlining and automation. Implementation of best practices raised performance standards and improved consistency of outcomes.",
                    "Stakeholder relationships strengthened through transparent communication and responsive engagement. Feedback mechanisms informed decision-making and demonstrated commitment to understanding diverse perspectives. These interactions built trust and supported collaborative problem-solving."
            }
    );

    // Text content methods
    public String getParagraph() {
        String[] paragraphs = PARAGRAPHS_BY_THEME.getOrDefault(theme, PARAGRAPHS_BY_THEME.get(Theme.DEFAULT));
        return paragraphs[random.nextInt(paragraphs.length)];
    }

    public String getSentence() {
        String[] sentences = SENTENCES_BY_THEME.getOrDefault(theme, SENTENCES_BY_THEME.get(Theme.DEFAULT));
        return sentences[random.nextInt(sentences.length)];
    }

    public List<String> getBulletPoints(int count) {
        String[] sentences = SENTENCES_BY_THEME.getOrDefault(theme, SENTENCES_BY_THEME.get(Theme.DEFAULT));
        List<String> points = new ArrayList<>();
        List<Integer> usedIndices = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            int idx;
            // Avoid duplicates if possible
            if (usedIndices.size() < sentences.length) {
                do {
                    idx = random.nextInt(sentences.length);
                } while (usedIndices.contains(idx));
                usedIndices.add(idx);
            } else {
                idx = random.nextInt(sentences.length);
            }
            points.add(sentences[idx]);
        }
        return points;
    }

    public String getReportTitle() {
        return getCompanyName() + " - " + getQuarter() + " " + getYear() + " Report";
    }

    public String getExecutiveSummary() {
        return String.format(
                "This report provides a comprehensive analysis of %s performance for %s %d. " +
                        "Key findings indicate %s growth in revenue with %s market expansion. " +
                        "The %s department has shown exceptional results, achieving %d%% of targets. " +
                        "Strategic initiatives in %s have positioned the company for continued success.",
                getCompanyName(), getQuarter(), getYear(),
                getBusinessBuzzword(), getBusinessBuzzword(),
                getDepartment(), 80 + random.nextInt(21),
                getIndustry()
        );
    }

    public String getMeetingAgenda() {
        StringBuilder sb = new StringBuilder();
        sb.append("Meeting Agenda - ").append(getDepartment()).append(" Department\n\n");
        sb.append("Date: ").append(LocalDate.now().plusDays(random.nextInt(30))).append("\n");
        sb.append("Attendees: ").append(getFullName()).append(", ").append(getFullName()).append(", ").append(getFullName()).append("\n\n");
        sb.append("Topics:\n");
        for (int i = 1; i <= 5; i++) {
            sb.append(i).append(". ").append(getSentence()).append("\n");
        }
        return sb.toString();
    }

    // Spreadsheet data methods
    public String[] getFinancialHeaders() {
        return new String[]{"Category", "Q1", "Q2", "Q3", "Q4", "Total", "YoY Growth"};
    }

    public String[] getExpenseCategories() {
        return switch (theme) {
            case FINANCIAL -> new String[]{"Trading Costs", "Compliance", "Technology", "Personnel", "Office Space", "Marketing", "Research", "Legal"};
            case ENTERTAINMENT -> new String[]{"Production Costs", "Talent Fees", "Marketing", "Distribution", "Post-Production", "Music Rights", "Insurance", "Travel"};
            case HEALTHCARE -> new String[]{"Medical Supplies", "Personnel", "Equipment", "Facilities", "Insurance", "IT Systems", "Research", "Compliance"};
            case TECHNOLOGY -> new String[]{"Cloud Infrastructure", "Personnel", "Software Licenses", "Hardware", "Security", "Marketing", "R&D", "Office"};
            case LEGAL -> new String[]{"Personnel", "Research Services", "Office Space", "Technology", "Marketing", "Insurance", "Travel", "Training"};
            case EDUCATION -> new String[]{"Faculty Salaries", "Facilities", "Technology", "Research", "Student Services", "Athletics", "Administration", "Marketing"};
            case RETAIL -> new String[]{"Inventory", "Personnel", "Rent", "Marketing", "Logistics", "Technology", "Insurance", "Utilities"};
            default -> new String[]{"Salaries", "Marketing", "Operations", "R&D", "Legal", "IT Infrastructure", "Travel", "Office Supplies"};
        };
    }

    public String[] getRevenueStreams() {
        return switch (theme) {
            case FINANCIAL -> new String[]{"Asset Management Fees", "Trading Revenue", "Advisory Fees", "Interest Income", "Underwriting", "Custody Services"};
            case ENTERTAINMENT -> new String[]{"Box Office", "Streaming", "Licensing", "Merchandise", "Live Events", "Advertising"};
            case HEALTHCARE -> new String[]{"Patient Services", "Insurance Payments", "Research Grants", "Pharmacy", "Laboratory", "Consulting"};
            case TECHNOLOGY -> new String[]{"Software Licenses", "SaaS Subscriptions", "Professional Services", "Support Contracts", "Hardware", "Training"};
            case LEGAL -> new String[]{"Hourly Fees", "Contingency Fees", "Retainers", "Consulting", "Document Review", "Expert Witness"};
            case EDUCATION -> new String[]{"Tuition", "Research Grants", "Donations", "Auxiliary Services", "Online Programs", "Continuing Education"};
            case RETAIL -> new String[]{"In-Store Sales", "E-commerce", "Wholesale", "Private Label", "Services", "Licensing"};
            default -> new String[]{"Product Sales", "Services", "Licensing", "Subscriptions", "Consulting", "Support Contracts"};
        };
    }

    public double[][] generateFinancialData(int rows) {
        double[][] data = new double[rows][6]; // Q1, Q2, Q3, Q4, Total, Growth
        for (int i = 0; i < rows; i++) {
            double base = 10000 + random.nextDouble() * 90000;
            data[i][0] = base * (0.9 + random.nextDouble() * 0.2);
            data[i][1] = base * (0.9 + random.nextDouble() * 0.2);
            data[i][2] = base * (0.9 + random.nextDouble() * 0.2);
            data[i][3] = base * (0.9 + random.nextDouble() * 0.2);
            data[i][4] = data[i][0] + data[i][1] + data[i][2] + data[i][3];
            data[i][5] = -20 + random.nextDouble() * 50; // -20% to +30% growth
        }
        return data;
    }

    // Presentation content methods
    public String getSlideTitle() {
        String[] titles = switch (theme) {
            case FINANCIAL -> new String[]{"Market Overview", "Portfolio Performance", "Risk Analysis", "Investment Strategy", "Regulatory Update", "Client Metrics", "Growth Opportunities", "Competitive Landscape", "Technology Initiatives", "Next Quarter Outlook"};
            case ENTERTAINMENT -> new String[]{"Creative Vision", "Audience Insights", "Content Pipeline", "Distribution Strategy", "Marketing Campaign", "Talent Updates", "Production Timeline", "Revenue Projections", "Partnership Opportunities", "Industry Trends"};
            case HEALTHCARE -> new String[]{"Patient Outcomes", "Clinical Excellence", "Quality Metrics", "Research Updates", "Regulatory Compliance", "Staff Development", "Technology Integration", "Community Impact", "Financial Performance", "Strategic Priorities"};
            case TECHNOLOGY -> new String[]{"Product Roadmap", "Technical Architecture", "User Metrics", "Security Posture", "Cloud Strategy", "Team Updates", "Innovation Pipeline", "Market Position", "Partner Ecosystem", "Growth Projections"};
            case LEGAL -> new String[]{"Case Portfolio", "Practice Highlights", "Client Success Stories", "Regulatory Updates", "Team Accomplishments", "Business Development", "Technology Investments", "Industry Insights", "Pro Bono Impact", "Strategic Direction"};
            case EDUCATION -> new String[]{"Academic Excellence", "Student Success", "Research Impact", "Faculty Achievements", "Campus Development", "Enrollment Trends", "Financial Sustainability", "Community Engagement", "Technology Innovation", "Future Vision"};
            case RETAIL -> new String[]{"Sales Performance", "Customer Insights", "Inventory Optimization", "Digital Transformation", "Store Operations", "Marketing ROI", "Supply Chain Updates", "Competitive Analysis", "Growth Strategy", "Seasonal Planning"};
            default -> new String[]{"Executive Overview", "Market Opportunity", "Our Solution", "Key Metrics", "Financial Highlights", "Team Structure", "Roadmap", "Competitive Analysis", "Growth Strategy", "Next Steps"};
        };
        return titles[random.nextInt(titles.length)];
    }

    public List<String> getSlideContent() {
        return getBulletPoints(4 + random.nextInt(3));
    }
}
