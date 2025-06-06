package namedEntity.heuristic;

import java.io.Serializable;
import java.util.Map;
// import java.util.Locale.Category;

import namedEntity.topics.*;
import namedEntity.categories.*;
import namedEntity.categories.Person.*;
import namedEntity.categories.Place.Address;
import namedEntity.categories.Place.City;
import namedEntity.categories.Place.Country;
import namedEntity.categories.Place.OtherPlace;
import namedEntity.categories.Place.Place;

public abstract class Heuristic implements Serializable {
    private static final long serialVersionUID = 1L;

private static final Map<String, Topic> topicMap = Map.ofEntries(
        // == People (PERSONA) ==
        // -- Subtype: LastName (mapped to Canonical Full Name) --
        Map.entry("Joe Biden", new Politics("Current President of the United States of America")),
        Map.entry("Donald Trump", new Politics("Former US President and prominent political figure")),
        Map.entry("Elon Musk", new Culture("CEO of Tesla, SpaceX and X (formerly Twitter), technological innovator")),
        Map.entry("Jeff Bezos", new Culture("Founder and former CEO of Amazon, entrepreneur")),
        Map.entry("Bill Gates", new Culture("Co-founder of Microsoft and philanthropist")),
        Map.entry("Mark Zuckerberg", new Culture("CEO of Meta Platforms (Facebook, Instagram, WhatsApp)")),
        Map.entry("Satya Nadella", new Culture("CEO of Microsoft, leading its transformation in cloud and AI")),
        Map.entry("Sundar Pichai", new Culture("CEO of Google and Alphabet Inc.")),
        Map.entry("Tim Cook", new Culture("CEO of Apple Inc.")),
        Map.entry("Steve Jobs", new Culture("Co-founder of Apple, pioneer of personal computing")),

        // -- Common Last Names (as OtherTopic, key remains as is if not in canonicalMap values) --
        Map.entry("Smith", new OtherTopic("Common surname of English origin")),
        Map.entry("Jones", new OtherTopic("Common surname of Welsh origin")),
        Map.entry("Williams", new OtherTopic("Common surname of Welsh or English origin")),
        Map.entry("Brown", new OtherTopic("Common surname referring to a color or complexion")),
        Map.entry("Davis", new OtherTopic("Common surname of Welsh origin")),
        Map.entry("Miller", new OtherTopic("Occupational surname, referring to a miller")),
        Map.entry("Wilson", new OtherTopic("Patronymic surname, 'son of Will'")),

        // -- Subtype: Name (Some mapped to Canonical Full Name, others generic) --
        // Note: Generic names like "Sarah" are kept if they don't map to a specific canonical person from your list.
        // Entries for "Joe", "Elon", etc. that map to full names are covered by the LastName entries above.
        // We prioritize the more specific Topic (e.g. Politics for Joe Biden over OtherTopic for "Joe").
        Map.entry("Sarah", new OtherTopic("Female given name")),
        Map.entry("Michael", new OtherTopic("Male given name")),
        Map.entry("Jennifer", new OtherTopic("Female given name")),
        Map.entry("David", new OtherTopic("Male given name")),
        // Generic first names that didn't map to a more specific entity:
        // "Jeff" (as Jeff Bezos is covered), "Bill" (as Bill Gates is covered), etc.
        // If "Mark" was only meant for Zuckerberg, it's covered. If generic "Mark", it would be:
        // Map.entry("Mark", new OtherTopic("Male given name")), // Assuming a generic Mark, not Zuckerberg.

        // -- Subtype: Title --
        Map.entry("Mr.", new OtherTopic("Courtesy title for men")),
        Map.entry("Ms.", new OtherTopic("Courtesy title for women, regardless of marital status")),
        Map.entry("Mrs.", new OtherTopic("Courtesy title for married women")),
        Map.entry("Dr.", new OtherTopic("Title for doctors (medical or PhD)")),
        Map.entry("Prof.", new OtherTopic("Title for university professors")),

        // == Places (LUGAR) ==
        // -- Subtype: Country (using canonical names like "United States of America") --
        Map.entry("United States of America", new Politics("United States of America, world power")), // Consolidated from USA, U.S., America
        Map.entry("China", new Politics("People's Republic of China, Asian economic and political power")),
        Map.entry("India", new Politics("Republic of India, South Asian country")),
        Map.entry("Germany", new Politics("Germany, Central European country")),
        Map.entry("United Kingdom", new Politics("United Kingdom, island country in Western Europe")), // Consolidated from UK, U.K.
        Map.entry("France", new Politics("France, Western European country")),
        Map.entry("Japan", new Politics("Japan, island country in East Asia")),
        Map.entry("Canada", new Politics("Canada, North American country")),
        Map.entry("Brazil", new Politics("Brazil, South American country")),
        Map.entry("Russia", new Politics("Russia, transcontinental country")),

        // -- Subtype: City (using canonical names like "New York City") --
        Map.entry("London", new Culture("Capital of England and the United Kingdom, financial and cultural center")),
        Map.entry("Paris", new Culture("Capital of France, known for its art, fashion, and culture")),
        Map.entry("Berlin", new Culture("Capital of Germany, historic and cultural city")),
        Map.entry("Tokyo", new Culture("Capital of Japan, technological and traditional metropolis")),
        Map.entry("Beijing", new Politics("Capital of China, political and cultural center")),
        Map.entry("Delhi", new Culture("Capital of India, important historical and metropolitan center")),
        Map.entry("Moscow", new Politics("Capital of Russia, political and cultural center")),
        Map.entry("Toronto", new Culture("Largest city in Canada, multicultural and financial center")),
        Map.entry("New York City", new Culture("New York City, global center of finance, culture, and media")), // Consolidated from NYC
        Map.entry("San Francisco", new Culture("San Francisco, technological and cultural center in California")), // Consolidated from SF
        Map.entry("Los Angeles", new Culture("Los Angeles, center of the entertainment industry")), // Consolidated from LA
        Map.entry("Chicago", new Culture("Major city in the US Midwest, known for its architecture")),
        Map.entry("Boston", new Culture("Historic city and educational center in the US")),
        Map.entry("Austin", new Culture("Capital of Texas, known for its music and tech scene")),
        Map.entry("Seattle", new Culture("City in Washington state, home to major tech companies")),

        // -- Subtype: Address --
        Map.entry("Main St", new OtherTopic("Common street name in English-speaking cities")),
        Map.entry("Fifth Avenue", new Culture("Famous avenue in New York, known for luxury shopping")),

        // -- Subtype: OtherPlace (States, Continents, Regions) --
        Map.entry("California", new Politics("US state, known for its economy and culture")),
        Map.entry("Texas", new Politics("US state, known for its energy industry and size")),
        Map.entry("Europe", new Culture("Continent with a rich history and cultural diversity")),
        Map.entry("Asia", new Culture("Largest and most populous continent, with great cultural diversity")),
        Map.entry("Silicon Valley", new Culture("Region in California, global center for high technology and innovation")),
        Map.entry("Wall Street", new Culture("Financial district of New York, symbol of American capitalism")),

        // == Organizations (ORGANIZACION) == (using canonical names like "Microsoft Corporation")
        Map.entry("Microsoft Corporation", new Culture("Multinational technology company, developer of Windows and Office")),
        Map.entry("Apple Inc.", new Culture("Multinational technology company, creator of the iPhone and Mac")),
        Map.entry("Google LLC", new Culture("Multinational technology company, known for its search engine and Android")),
        Map.entry("Alphabet Inc.", new Culture("Technology conglomerate, parent company of Google")),
        Map.entry("Amazon.com, Inc.", new Culture("Multinational e-commerce and cloud computing company")),
        Map.entry("Meta Platforms, Inc.", new Culture("Technology company, owner of Facebook, Instagram, and WhatsApp")), // Consolidated from "Meta" and "Facebook" (company)
        Map.entry("Tesla, Inc.", new Culture("Electric vehicle and clean energy company")),
        Map.entry("Nvidia", new Culture("Designer of graphics processing units (GPUs) and AI company")), // Assuming "Nvidia" is canonical if not further specified in canonicalMap
        Map.entry("Intel", new Culture("Manufacturer of microprocessors and semiconductors")), // Assuming "Intel" is canonical
        Map.entry("International Business Machines Corporation", new Culture("Multinational technology and consulting company")),
        Map.entry("Oracle", new Culture("Software company, specializing in databases and cloud systems")), // Assuming "Oracle" is canonical
        Map.entry("Salesforce, Inc.", new Culture("Cloud-based software company, specializing in CRM")),
        Map.entry("HubSpot", new Culture("Software company for inbound marketing, sales, and customer service")), // Assuming "HubSpot" is canonical
        Map.entry("The New York Times Company", new Culture("The New York Times, influential global newspaper")),
        Map.entry("Reddit", new Culture("Social news aggregation and discussion website")), // Assuming "Reddit" is canonical
        Map.entry("NASA", new Politics("United States space agency")), // Acronyms kept if they are the common canonical form
        Map.entry("FBI", new Politics("Federal Bureau of Investigation of the US")),
        Map.entry("CIA", new Politics("Central Intelligence Agency of the US")),
        Map.entry("SEC", new Politics("US Securities and Exchange Commission, financial regulator")),
        Map.entry("EU", new Politics("European Union, political and economic organization of European countries")),
        Map.entry("UN", new Politics("United Nations, international organization")),
        Map.entry("NATO", new Politics("North Atlantic Treaty Organization, military alliance")),
        Map.entry("Inc.", new OtherTopic("Suffix indicating a corporation (Incorporated)")),
        Map.entry("Ltd.", new OtherTopic("Suffix indicating a limited liability company (Limited)")),
        Map.entry("Corp.", new OtherTopic("Abbreviation for Corporation")),
        Map.entry("LLC", new OtherTopic("Limited Liability Company")),
        Map.entry("Startup", new Culture("Emerging company with high growth potential and innovation")),
        Map.entry("Y Combinator Management, LLC", new Culture("World-renowned startup accelerator")),
        Map.entry("Techstars", new Culture("Global network for startup investment and acceleration")), // Assuming "Techstars" is canonical

        // == Products (PRODUCTO) == (using canonical names like "Apple iPhone")
        Map.entry("Apple iPhone", new Culture("Smartphone designed by Apple")),
        Map.entry("Apple iPad", new Culture("Tablet designed by Apple")),
        Map.entry("Apple MacBook", new Culture("Line of laptop computers by Apple")),
        Map.entry("Microsoft Windows", new Culture("Operating system developed by Microsoft")),
        Map.entry("Microsoft Office", new Culture("Productivity software suite by Microsoft")),
        Map.entry("Microsoft Excel", new Culture("Spreadsheet software from Microsoft Office")),
        Map.entry("Microsoft Word", new Culture("Word processor from Microsoft Office")),
        Map.entry("Microsoft PowerPoint", new Culture("Presentation software from Microsoft Office")),
        Map.entry("Android", new Culture("Mobile operating system developed by Google")), // "Android" itself is often canonical
        Map.entry("Google Pixel", new Culture("Line of consumer electronic devices by Google")),
        Map.entry("Samsung Galaxy", new Culture("Line of mobile devices by Samsung")),
        Map.entry("Amazon Web Services", new Culture("Amazon Web Services, cloud computing platform")),
        Map.entry("Microsoft Azure", new Culture("Microsoft Azure, cloud computing platform")),
        Map.entry("Google Cloud Platform", new Culture("Google Cloud Platform, cloud computing platform")),
        Map.entry("OpenAI ChatGPT", new Culture("AI language model developed by OpenAI")),
        Map.entry("GPT-4", new Culture("Large multimodal language model by OpenAI")), // Assuming "GPT-4" is canonical
        Map.entry("Model S", new Culture("Luxury electric sedan produced by Tesla")), // Assuming "Model S" is canonical (Tesla Model S)
        Map.entry("Sony PlayStation", new Culture("Brand of video game consoles by Sony")),
        Map.entry("Microsoft Xbox", new Culture("Brand of video game consoles by Microsoft")),
        Map.entry("Photoshop", new Culture("Raster graphics editing software by Adobe")), // Assuming "Photoshop" is canonical
        Map.entry("Salesforce CRM", new Culture("Customer relationship management platform by Salesforce")), // Assuming "Salesforce CRM" is canonical
        Map.entry("HubSpot CRM", new Culture("CRM software by HubSpot")), // Assuming "HubSpot CRM" is canonical
        Map.entry("Software as a Service", new Culture("Software as a Service, software distribution model")),
        Map.entry("Bitcoin", new Culture("Decentralized cryptocurrency, the first of its kind")), // Assuming "Bitcoin" is canonical
        Map.entry("Ethereum", new Culture("Decentralized blockchain platform with smart contract functionality")), // Assuming "Ethereum" is canonical

        // == Events (EVENTO) ==
        Map.entry("Initial Public Offering", new Culture("Initial Public Offering, process by which a private company sells shares to the public")),
        Map.entry("WWDC", new Culture("Apple Worldwide Developers Conference")), // Acronyms often canonical for events
        Map.entry("Google I/O", new Culture("Google's annual developer conference")),
        Map.entry("CES", new Culture("Consumer Electronics Show, annual consumer technology trade show")),
        Map.entry("Olympics", new Sports("Olympic Games, major international sporting event")),
        Map.entry("World Cup", new Sports("FIFA World Cup, international football tournament")),
        Map.entry("Super Bowl", new Sports("Final championship game of the National Football League (NFL)")),
        Map.entry("summit", new Politics("Summit or high-level meeting between leaders")),
        Map.entry("conference", new OtherTopic("Conference or meeting for discussion or information exchange")),
        Map.entry("webinar", new Culture("Seminar or presentation conducted online")),
        Map.entry("launch", new Culture("Launch of a new product, service, or initiative")),
        Map.entry("election", new Politics("Formal decision-making process to elect individuals to public office")),

        // == Dates (FECHA) ==
        Map.entry("Monday", new OtherTopic("Monday, first or second day of the week")),
        Map.entry("Tuesday", new OtherTopic("Tuesday, second or third day of the week")),
        Map.entry("Wednesday", new OtherTopic("Wednesday, third or fourth day of the week")),
        Map.entry("Thursday", new OtherTopic("Thursday, fourth or fifth day of the week")),
        Map.entry("Friday", new OtherTopic("Friday, fifth or sixth day of the week")),
        Map.entry("Saturday", new OtherTopic("Saturday, sixth or seventh day of the week")),
        Map.entry("Sunday", new OtherTopic("Sunday, seventh or first day of the week")),
        Map.entry("January", new OtherTopic("January, first month of the year")),
        Map.entry("February", new OtherTopic("February, second month of the year")),
        Map.entry("March", new OtherTopic("March, third month of the year")),
        Map.entry("April", new OtherTopic("April, fourth month of the year")),
        Map.entry("May", new OtherTopic("May, fifth month of the year")),
        Map.entry("June", new OtherTopic("June, sixth month of the year")),
        Map.entry("July", new OtherTopic("July, seventh month of the year")),
        Map.entry("August", new OtherTopic("August, eighth month of the year")),
        Map.entry("September", new OtherTopic("September, ninth month of the year")),
        Map.entry("October", new OtherTopic("October, tenth month of the year")),
        Map.entry("November", new OtherTopic("November, eleventh month of the year")),
        Map.entry("December", new OtherTopic("December, twelfth month of the year")),
        Map.entry("Christmas", new Culture("Christmas, Christian holiday commemorating the birth of Jesus")),
        Map.entry("Easter", new Culture("Easter, Christian holiday commemorating the resurrection of Jesus")),
        Map.entry("Thanksgiving", new Culture("Thanksgiving, holiday celebrated primarily in the US and Canada")),
        Map.entry("New Year", new Culture("New Year, celebration of the beginning of a new calendar year")),
        Map.entry("Q1", new OtherTopic("First quarter of the fiscal or calendar year")),
        Map.entry("Q2", new OtherTopic("Second quarter of the fiscal or calendar year")),
        Map.entry("Q3", new OtherTopic("Third quarter of the fiscal or calendar year")),
        Map.entry("Q4", new OtherTopic("Fourth quarter of the fiscal or calendar year")),

        // == Other (OTRO) - Concepts, Technologies == (using canonical names like "Artificial Intelligence")
        Map.entry("Artificial Intelligence", new Culture("Artificial Intelligence, field of computer science")),
        Map.entry("Machine Learning", new Culture("Machine Learning, subfield of AI")),
        Map.entry("Cloud", new Culture("Cloud computing, paradigm for delivering IT services")), // Assuming "Cloud" is canonical enough here
        Map.entry("Internet", new Culture("Global network of interconnected computers")),
        Map.entry("Web", new Culture("World Wide Web, system for distributing hypertext documents")),
        Map.entry("Big Data", new Culture("Large volumes of data and the technologies to analyze them")),
        Map.entry("Blockchain", new Culture("Distributed ledger technology, basis of cryptocurrencies")),
        Map.entry("Cryptocurrency", new Culture("Digital or virtual currency secured by cryptography")),
        Map.entry("Search Engine Optimization", new Culture("Search Engine Optimization")),
        Map.entry("Search Engine Marketing", new Culture("Search Engine Marketing")),
        Map.entry("Customer Relationship Management", new Culture("Customer Relationship Management")),
        Map.entry("Business-to-Business", new OtherTopic("Business-to-Business, business model between companies")),
        Map.entry("Business-to-Consumer", new OtherTopic("Business-to-Consumer, business model from company to consumer")),
        Map.entry("Return on Investment", new OtherTopic("Return on Investment, financial metric")),
        Map.entry("Key Performance Indicator", new OtherTopic("Key Performance Indicator")),
        Map.entry("Minimum Viable Product", new Culture("Minimum Viable Product, concept in product development"))
    );

	private static final Map<String, Category> categoryMap = Map.ofEntries(
        // --- People (Politics, Business, Technology) ---
        Map.entry("Joe Biden", new Person(new LastName("Biden"), new Name("Joe"), new Title("President of the United States", "Joe Biden"))),
        Map.entry("Donald Trump", new Person(new LastName("Trump"), new Name("Donald"), new Title("Former President of the United States", "Donald Trump"))),
        Map.entry("Kamala Harris", new Person(new LastName("Harris"), new Name("Kamala"), new Title("Vice President of the United States", "Kamala Harris"))),
        Map.entry("Vladimir Putin", new Person(new LastName("Putin"), new Name("Vladimir"), new Title("President of Russia", "Vladimir Putin"))),
        Map.entry("Xi Jinping", new Person(new LastName("Jinping"), new Name("Xi"), new Title("President of China", "Xi Jinping"))),
        Map.entry("Volodymyr Zelenskyy", new Person(new LastName("Zelenskyy"), new Name("Volodymyr"), new Title("President of Ukraine", "Volodymyr Zelenskyy"))),
        Map.entry("Ursula von der Leyen", new Person(new LastName("von der Leyen"), new Name("Ursula"), new Title("President of the European Commission", "Ursula von der Leyen"))),
        Map.entry("Elon Musk", new Person(new LastName("Musk"), new Name("Elon"), new Title("CEO of Tesla and SpaceX, Owner of X", "Elon Musk"))),
        Map.entry("Tim Cook", new Person(new LastName("Cook"), new Name("Tim"), new Title("CEO of Apple", "Tim Cook"))),
        Map.entry("Satya Nadella", new Person(new LastName("Nadella"), new Name("Satya"), new Title("CEO of Microsoft", "Satya Nadella"))),
        Map.entry("Sundar Pichai", new Person(new LastName("Pichai"), new Name("Sundar"), new Title("CEO of Alphabet and Google", "Sundar Pichai"))),
        Map.entry("Mark Zuckerberg", new Person(new LastName("Zuckerberg"), new Name("Mark"), new Title("CEO of Meta Platforms", "Mark Zuckerberg"))),
        Map.entry("Jerome Powell", new Person(new LastName("Powell"), new Name("Jerome"), new Title("Chair of the Federal Reserve", "Jerome Powell"))),
        Map.entry("Jensen Huang", new Person(new LastName("Huang"), new Name("Jensen"), new Title("CEO of Nvidia", "Jensen Huang"))),
        Map.entry("Sam Altman", new Person(new LastName("Altman"), new Name("Sam"), new Title("CEO of OpenAI", "Sam Altman"))),

        // --- Places (Cities, Countries, Regions) ---
        Map.entry("Washington D.C.", new Place("Washington D.C.", new Address("1600 Pennsylvania Avenue NW (Example)"), new City("Washington"), new Country("USA"))),
        Map.entry("New York City", new Place("New York City", new Address("Times Square (Example)"), new City("New York"), new Country("USA"))),
        Map.entry("London", new Place("London", new Address("10 Downing Street (Example)"), new City("London"), new Country("United Kingdom"))),
        Map.entry("Paris", new Place("Paris", new Address("Eiffel Tower (Example)"), new City("Paris"), new Country("France"))),
        Map.entry("Berlin", new Place("Berlin", new Address("Brandenburg Gate (Example)"), new City("Berlin"), new Country("Germany"))),
        Map.entry("Beijing", new Place("Beijing", new Address("Tiananmen Square (Example)"), new City("Beijing"), new Country("China"))),
        Map.entry("Tokyo", new Place("Tokyo", new Address("Shibuya Crossing (Example)"), new City("Tokyo"), new Country("Japan"))),
        Map.entry("Moscow", new Place("Moscow", new Address("Red Square (Example)"), new City("Moscow"), new Country("Russia"))),
        Map.entry("Kyiv", new Place("Kyiv", new Address("Maidan Nezalezhnosti (Example)"), new City("Kyiv"), new Country("Ukraine"))),
        Map.entry("Brussels", new Place("Brussels", new Address("Grand Place (Example)"), new City("Brussels"), new Country("Belgium"))), // EU Headquarters
        Map.entry("Silicon Valley", new OtherPlace("Silicon Valley - Global center for high technology and innovation")),
        Map.entry("Wall Street", new OtherPlace("Wall Street - Financial district of New York City")),
        Map.entry("Gaza Strip", new OtherPlace("Gaza Strip - Palestinian exclave on the eastern coast of the Mediterranean Sea")),
        Map.entry("Taiwan", new Place("Taiwan", new Address("Taipei 101 (Example)"), new City("Taipei"), new Country("Taiwan (ROC)"))), // Politically sensitive, representing as a de facto state

        // --- Organizations (Companies, Political, NGOs) ---
        Map.entry("Apple", new Organization("Apple Inc. - Multinational technology company")),
        Map.entry("Microsoft", new Organization("Microsoft Corporation - Multinational technology corporation")),
        Map.entry("Google", new Organization("Google LLC - Multinational technology company specializing in Internet-related services")),
        Map.entry("Amazon", new Organization("Amazon.com, Inc. - Multinational technology company focusing on e-commerce, cloud computing, online advertising, digital streaming, and artificial intelligence")),
        Map.entry("Meta Platforms", new Organization("Meta Platforms, Inc. - Parent company of Facebook, Instagram, WhatsApp")),
        Map.entry("Tesla", new Organization("Tesla, Inc. - Electric vehicle and clean energy company")),
        Map.entry("Nvidia", new Organization("Nvidia Corporation - Technology company known for GPUs and AI hardware/software")),
        Map.entry("OpenAI", new Organization("OpenAI - Artificial intelligence research and deployment company")),
        Map.entry("SpaceX", new Organization("SpaceX - Aerospace manufacturer and space transportation services company")),
        Map.entry("United Nations", new Organization("United Nations (UN) - Intergovernmental organization promoting international cooperation")),
        Map.entry("NATO", new Organization("North Atlantic Treaty Organization (NATO) - Intergovernmental military alliance")),
        Map.entry("European Union", new Organization("European Union (EU) - Political and economic union of member states located primarily in Europe")),
        Map.entry("World Health Organization", new Organization("World Health Organization (WHO) - Specialized agency of the United Nations responsible for international public health")),
        Map.entry("Federal Reserve", new Organization("Federal Reserve System (The Fed) - Central banking system of the United States")),
        Map.entry("International Monetary Fund", new Organization("International Monetary Fund (IMF) - International financial institution")),
        Map.entry("Y Combinator", new Organization("Y Combinator - American technology startup accelerator")),

        // --- Products & Services ---
        Map.entry("iPhone", new Product("iPhone - Line of smartphones by Apple")),
        Map.entry("Android", new Product("Android - Mobile operating system by Google")),
        Map.entry("Windows", new Product("Microsoft Windows - Operating system by Microsoft")),
        Map.entry("ChatGPT", new Product("ChatGPT - AI chatbot developed by OpenAI")),
        Map.entry("Google Search", new Product("Google Search - Web search engine by Google")),
        Map.entry("Amazon Web Services", new Product("Amazon Web Services (AWS) - Cloud computing platform by Amazon")),
        Map.entry("Tesla Model Y", new Product("Tesla Model Y - Electric compact crossover utility vehicle by Tesla")),
        Map.entry("Vision Pro", new Product("Apple Vision Pro - Mixed reality headset by Apple")),
        Map.entry("Starlink", new Product("Starlink - Satellite internet constellation operated by SpaceX")),

        // --- Events (Recurring, Specific) ---
        Map.entry("WWDC", new Event("Apple Worldwide Developers Conference (WWDC)")),
        Map.entry("Google I/O", new Event("Google I/O - Annual developer conference by Google")),
        Map.entry("Microsoft Build", new Event("Microsoft Build - Annual conference event by Microsoft, aimed at software engineers and web developers")),
        Map.entry("CES", new Event("Consumer Electronics Show (CES) - Annual trade show for consumer technologies")),
        Map.entry("Olympic Games", new Event("Olympic Games - International multi-sport event held every four years")),
        Map.entry("FIFA World Cup", new Event("FIFA World Cup - International men's football tournament")),
        Map.entry("G7 Summit", new Event("G7 Summit - Annual meeting of leaders from seven of the world's advanced economies")),
        Map.entry("COP Climate Change Conference", new Event("Conference of the Parties (COP) - Annual UN climate change conference")),
        Map.entry("Davos Forum", new Event("World Economic Forum Annual Meeting in Davos")),
        Map.entry("War in Ukraine", new Event("Ongoing armed conflict between Russia and Ukraine")), // More of an ongoing situation, but fits Event

        // --- Dates & Time-related Entities ---
        Map.entry("Monday", new DateEntity("Day of the week", "Monday")),
        Map.entry("January", new DateEntity("Month of the year", "January")),
        Map.entry("Q1", new DateEntity("Fiscal Quarter", "First Quarter")),
        Map.entry("2024", new DateEntity("Year", "2024")),
        Map.entry("New Year's Day", new DateEntity("Holiday", "January 1st")),
        Map.entry("Christmas", new DateEntity("Holiday", "December 25th")),

        // --- Other (Concepts, Phenomena, Broad Topics) ---
        Map.entry("Artificial Intelligence", new Other("Artificial Intelligence (AI) - Intelligence demonstrated by machines, as opposed to natural intelligence displayed by animals including humans.")),
        Map.entry("Climate Change", new Other("Climate Change - Long-term shifts in temperatures and weather patterns, largely driven by human activities.")),
        Map.entry("Inflation", new Other("Inflation - Rate of increase in prices over a given period of time, leading to a fall in purchasing power.")),
        Map.entry("Recession", new Other("Recession - Significant, widespread, and prolonged downturn in economic activity.")),
        Map.entry("Cryptocurrency", new Other("Cryptocurrency - Digital or virtual currency secured by cryptography.")),
        Map.entry("Blockchain", new Other("Blockchain - Distributed ledger technology that underlies cryptocurrencies.")),
        Map.entry("Quantum Computing", new Other("Quantum Computing - Type of computation that harnesses the collective properties of quantum states.")),
        Map.entry("Social Media", new Other("Social Media - Interactive technologies that facilitate the creation and sharing of information, ideas, interests, and other forms of expression through virtual communities and networks.")),
        Map.entry("Venture Capital", new Other("Venture Capital (VC) - Form of private equity financing provided by venture capital firms or funds to startups, early-stage, and emerging companies.")),
        Map.entry("Startups", new Other("Startups - Young companies founded to develop a unique product or service, bring it to market and make it irresistible and irreplaceable for customers."))
    );
	
	private static final Map<String, String> canonicalMap = Map.ofEntries(
            // People
            Map.entry("Biden", "Joe Biden"),
            Map.entry("Joe", "Joe Biden"), // Ambiguous, but for example
            Map.entry("J. Biden", "Joe Biden"),
            Map.entry("President Biden", "Joe Biden"),
            Map.entry("Trump", "Donald Trump"),
            Map.entry("Donald", "Donald Trump"),
            Map.entry("D. Trump", "Donald Trump"),
			Map.entry("Donald Trump", "Donald Trump"),
			Map.entry("Trumps", "Donald Trump"),
            Map.entry("President Trump", "Donald Trump"),
            Map.entry("Musk", "Elon Musk"),
            Map.entry("Elon", "Elon Musk"),
            Map.entry("Bezos", "Jeff Bezos"),
            Map.entry("Jeff", "Jeff Bezos"),
            Map.entry("Gates", "Bill Gates"),
            Map.entry("Bill", "Bill Gates"),
            Map.entry("Zuckerberg", "Mark Zuckerberg"),
            Map.entry("Mark", "Mark Zuckerberg"), // Ambiguous alone
            Map.entry("Nadella", "Satya Nadella"),
            Map.entry("Satya", "Satya Nadella"),
            Map.entry("Pichai", "Sundar Pichai"),
            Map.entry("Sundar", "Sundar Pichai"),
            Map.entry("Cook", "Tim Cook"),
            Map.entry("Tim", "Tim Cook"), // Ambiguous alone
            Map.entry("Jobs", "Steve Jobs"),
            Map.entry("Steve", "Steve Jobs"), // Ambiguous alone

            // Organizations
            Map.entry("Google", "Google LLC"),
            Map.entry("Alphabet", "Alphabet Inc."),
            Map.entry("Amazon", "Amazon.com, Inc."),
            Map.entry("AMZN", "Amazon.com, Inc."),
            Map.entry("Apple", "Apple Inc."),
            Map.entry("AAPL", "Apple Inc."),
            Map.entry("Microsoft", "Microsoft Corporation"),
            Map.entry("MSFT", "Microsoft Corporation"),
            Map.entry("Meta", "Meta Platforms, Inc."),
            Map.entry("Facebook", "Meta Platforms, Inc."), // Facebook (the company) is now Meta
            Map.entry("FB", "Meta Platforms, Inc."),
            Map.entry("Tesla", "Tesla, Inc."),
            Map.entry("TSLA", "Tesla, Inc."),
            Map.entry("IBM", "International Business Machines Corporation"),
            Map.entry("Salesforce", "Salesforce, Inc."),
            Map.entry("CRM", "Salesforce, Inc."), // If CRM refers to Salesforce the company
            Map.entry("NYT", "The New York Times Company"),
            Map.entry("The New York Times", "The New York Times Company"),
            Map.entry("Y Combinator", "Y Combinator Management, LLC"),
            Map.entry("YC", "Y Combinator Management, LLC"),

            // Places
            Map.entry("USA", "United States of America"),
            Map.entry("U.S.", "United States of America"),
            Map.entry("U.S.A.", "United States of America"),
            Map.entry("America", "United States of America"), // Context dependent
            Map.entry("UK", "United Kingdom"),
            Map.entry("U.K.", "United Kingdom"),
            Map.entry("Great Britain", "United Kingdom"),
            Map.entry("NYC", "New York City"),
            Map.entry("New York", "New York City"), // If context is city
            Map.entry("SF", "San Francisco"),
            Map.entry("LA", "Los Angeles"),
            Map.entry("Vegas", "Las Vegas"),
            Map.entry("Silicon Valley", "Silicon Valley"), // Already canonical enough

            // Products
            Map.entry("iPhone", "Apple iPhone"),
            Map.entry("iPad", "Apple iPad"),
            Map.entry("Mac", "Apple Macintosh"),
            Map.entry("MacBook", "Apple MacBook"),
            Map.entry("Windows", "Microsoft Windows"),
            Map.entry("Office", "Microsoft Office"),
            Map.entry("Excel", "Microsoft Excel"),
            Map.entry("Word", "Microsoft Word"),
            Map.entry("PowerPoint", "Microsoft PowerPoint"),
            Map.entry("Pixel", "Google Pixel"),
            Map.entry("Galaxy", "Samsung Galaxy"),
            Map.entry("AWS", "Amazon Web Services"),
            Map.entry("Azure", "Microsoft Azure"),
            Map.entry("GCP", "Google Cloud Platform"),
            Map.entry("GPT", "Generative Pre-trained Transformer"), // Concept/technology name
            Map.entry("ChatGPT", "OpenAI ChatGPT"),
            Map.entry("PlayStation", "Sony PlayStation"),
            Map.entry("PS5", "Sony PlayStation 5"),
            Map.entry("Xbox", "Microsoft Xbox"),

            // Concepts / Other (can also be mapped if they have common abbreviations)
            Map.entry("AI", "Artificial Intelligence"),
            Map.entry("ML", "Machine Learning"),
            Map.entry("GenAI", "Generative Artificial Intelligence"),
            Map.entry("SEO", "Search Engine Optimization"),
            Map.entry("SEM", "Search Engine Marketing"),
            Map.entry("SaaS", "Software as a Service"),
            Map.entry("PaaS", "Platform as a Service"),
            Map.entry("IaaS", "Infrastructure as a Service"),
            Map.entry("B2B", "Business-to-Business"),
            Map.entry("B2C", "Business-to-Consumer"),
            Map.entry("ROI", "Return on Investment"),
            Map.entry("KPI", "Key Performance Indicator"),
            Map.entry("MVP", "Minimum Viable Product"),
            Map.entry("VC", "Venture Capital"),
            Map.entry("IPO", "Initial Public Offering") // Event, but acronym can be canonicalized
    );

	public String getCanonical(String origin){
		String res = canonicalMap.get(origin);
		
		if (res == null){
			return origin;
		}

		return res;
	}

	public Category getCategory(String entity) {
		Category res = categoryMap.get(entity);

		if (res == null){
			res = new Other("No category found");
		}

		return res;
	}


	public Topic getTopic(String entity) {
		Topic res = topicMap.get(entity);

		if (res == null){
			res = new OtherTopic("No topic found");
		}

		return res;
	}


	public abstract boolean isEntity(String word);
}
