package com.example.util

data class TranslationGlossaryItem(
    val arabic: String,
    val english: String,
    val french: String,
    val subject: String,
    val category: String = "عام"
)

object SmartTranslatorHelper {

    val educationalGlossary: List<TranslationGlossaryItem> = listOf(
        // 0. App & Tool Names (أسماء الأدوات والتطبيق)
        TranslationGlossaryItem("هاكر التدريس", "The Hacker - Teacher Planner", "The Hacker - Planificateur Enseignant", "التطبيق"),
        TranslationGlossaryItem("المترجم", "Translator", "Traducteur", "الأدوات"),
        TranslationGlossaryItem("مترجم المعلم", "Teacher Translator", "Traducteur Enseignant", "الأدوات"),
        TranslationGlossaryItem("مترجم المعلم الفوري", "Smart Teacher Translator", "Traducteur Pédagogique Intelligent", "الأدوات"),
        TranslationGlossaryItem("الترجمة", "Translation", "Traduction", "الأدوات"),
        TranslationGlossaryItem("ترجمة", "Translate", "Traduire", "الأدوات"),
        TranslationGlossaryItem("اسم", "Name", "Nom", "عام"),
        TranslationGlossaryItem("اسماء", "Names", "Noms", "عام"),
        TranslationGlossaryItem("أسماء الأدوات", "Tool Names", "Noms des Outils", "الأدوات"),
        TranslationGlossaryItem("أداة", "Tool", "Outil", "الأدوات"),
        TranslationGlossaryItem("أدوات", "Tools", "Outils", "الأدوات"),
        TranslationGlossaryItem("أدوات المعلم", "Teacher Tools", "Outils Enseignant", "الأدوات"),
        TranslationGlossaryItem("الكلام", "Speech / Words", "Paroles / Mots", "عام"),
        TranslationGlossaryItem("كلام", "Words / Speech", "Mots / Discours", "عام"),
        TranslationGlossaryItem("نص", "Text", "Texte", "عام"),
        TranslationGlossaryItem("لا تعمل", "Not working / Does not work", "Ne fonctionne pas", "عام"),
        TranslationGlossaryItem("يعمل", "Works", "Fonctionne", "عام"),
        TranslationGlossaryItem("لا يترجم", "Is not translating", "Ne traduit pas", "عام"),
        TranslationGlossaryItem("المسطرة", "Ruler", "Règle", "الأدوات"),
        TranslationGlossaryItem("البرجل", "Compass", "Compas", "الأدوات"),
        TranslationGlossaryItem("البرجل الهندسي", "Geometric Compass", "Compas Géométrique", "الأدوات"),
        TranslationGlossaryItem("المنقلة", "Protractor", "Rapporteur", "الأدوات"),
        TranslationGlossaryItem("أشكال ثنائية 2D", "2D Shapes", "Formes 2D", "الأدوات"),
        TranslationGlossaryItem("مجسمات ثلاثية 3D", "3D Solids", "Solides 3D", "الأدوات"),
        TranslationGlossaryItem("تحريك وتكبير", "Pan & Zoom", "Déplacer & Zoomer", "الأدوات"),
        TranslationGlossaryItem("قلم ذكي", "Smart Pen", "Stylet Intelligent", "الأدوات"),
        TranslationGlossaryItem("قلم", "Pen", "Stylo", "الأدوات"),
        TranslationGlossaryItem("تظليل", "Highlighter", "Surligneur", "الأدوات"),
        TranslationGlossaryItem("مؤشر ليزر", "Laser Pointer", "Pointeur Laser", "الأدوات"),
        TranslationGlossaryItem("ممحاة", "Eraser", "Gomme", "الأدوات"),
        TranslationGlossaryItem("تسجيل الصوت", "Voice Studio / Audio Record", "Studio Vocal", "الأدوات"),
        TranslationGlossaryItem("تصوير الواجب", "Homework Scanner", "Scanner de Devoir", "الأدوات"),
        TranslationGlossaryItem("القرعة الذكية", "Random Student Picker", "Tirage au Sort", "الأدوات"),
        TranslationGlossaryItem("مؤقت الحصة", "Class Timer", "Minuteur de Classe", "الأدوات"),
        TranslationGlossaryItem("حاسبة الدرجات", "Grade Calculator", "Calculateur de Notes", "الأدوات"),
        TranslationGlossaryItem("سجل المذكرات", "Booklet Tracker", "Gestionnaire de Fascicules", "الأدوات"),
        TranslationGlossaryItem("بورتفوليو المعلم", "Teacher Portfolio", "Portfolio Enseignant", "الأدوات"),
        TranslationGlossaryItem("مركز الطباعة", "Print Hub", "Centre d'Impression", "الأدوات"),
        TranslationGlossaryItem("رسائل جاهزة", "Ready Messages", "Modèles de Messages", "الأدوات"),
        TranslationGlossaryItem("استخراج الصفحة كصورة", "Export Page as Image", "Exporter Page en Image", "الأدوات"),
        TranslationGlossaryItem("تحويل الملف كامل لصور", "Convert Full PDF to Images", "Convertir PDF en Images", "الأدوات"),
        TranslationGlossaryItem("تحديد جودة الصور", "Image Quality", "Qualité d'Image", "الأدوات"),
        TranslationGlossaryItem("جودة فائقة (300 DPI)", "High Quality (300 DPI)", "Haute Qualité (300 DPI)", "الأدوات"),
        TranslationGlossaryItem("جودة متوازنة (150 DPI)", "Medium Quality (150 DPI)", "Qualité Moyenne (150 DPI)", "الأدوات"),
        TranslationGlossaryItem("جودة خفيفة (96 DPI)", "Standard Quality (96 DPI)", "Qualité Standard (96 DPI)", "الأدوات"),
        TranslationGlossaryItem("مشاركة الصور", "Share Images", "Partager les Images", "الأدوات"),
        TranslationGlossaryItem("حفظ في الاستوديو", "Save to Gallery", "Enregistrer dans Galerie", "الأدوات"),
        TranslationGlossaryItem("تحديد الصف الدراسي", "Select Grade / Stage", "Choisir la Classe", "الأدوات"),

        // 1. Mathematics (الرياضيات)
        TranslationGlossaryItem("معادلة خطية", "Linear Equation", "Équation linéaire", "الرياضيات"),
        TranslationGlossaryItem("دالة تربيعية", "Quadratic Function", "Fonction quadratique", "الرياضيات"),
        TranslationGlossaryItem("حساب التفاضل والتكامل", "Calculus", "Calcul différentiel et intégral", "الرياضيات"),
        TranslationGlossaryItem("مثلث قائم الزاوية", "Right-angled Triangle", "Triangle rectangle", "الرياضيات"),
        TranslationGlossaryItem("مبرهنة فيثاغورس", "Pythagorean Theorem", "Théorème de Pythagore", "الرياضيات"),
        TranslationGlossaryItem("نصف القطر", "Radius", "Rayon", "الرياضيات"),
        TranslationGlossaryItem("مساحة السطح", "Surface Area", "Aire de surface", "الرياضيات"),
        TranslationGlossaryItem("المتجهات", "Vectors", "Vecteurs", "الرياضيات"),
        TranslationGlossaryItem("المصفوفات", "Matrices", "Matrices", "الرياضيات"),
        TranslationGlossaryItem("الاحتمالات والإحصاء", "Probability and Statistics", "Probabilités et statistiques", "الرياضيات"),
        TranslationGlossaryItem("الزاوية الحادة", "Acute Angle", "Angle aigu", "الرياضيات"),
        TranslationGlossaryItem("الزاوية المنفرجة", "Obtuse Angle", "Angle obtus", "الرياضيات"),
        TranslationGlossaryItem("الزاوية القائمة", "Right Angle", "Angle droit", "الرياضيات"),
        TranslationGlossaryItem("المحيط", "Perimeter", "Périmètre", "الرياضيات"),
        TranslationGlossaryItem("المساحة", "Area", "Aire / Surface", "الرياضيات"),
        TranslationGlossaryItem("الحجم", "Volume", "Volume", "الرياضيات"),
        TranslationGlossaryItem("الكسور العادية", "Fractions", "Fractions", "الرياضيات"),
        TranslationGlossaryItem("النسبة والتناسب", "Ratio and Proportion", "Rapports et proportions", "الرياضيات"),
        TranslationGlossaryItem("الجذر التربيعي", "Square Root", "Racine carrée", "الرياضيات"),
        TranslationGlossaryItem("المتواليات الحسابية", "Arithmetic Progressions", "Suites arithmétiques", "الرياضيات"),
        TranslationGlossaryItem("المتواليات الهندسية", "Geometric Progressions", "Suites géométriques", "الرياضيات"),
        TranslationGlossaryItem("اللوغاريتمات", "Logarithms", "Logarithmes", "الرياضيات"),
        TranslationGlossaryItem("حساب المثلثات", "Trigonometry", "Trigonométrie", "الرياضيات"),

        // 2. Physics (الفيزياء)
        TranslationGlossaryItem("السرعة المتجهة", "Velocity", "Vitesse vectorielle", "الفيزياء"),
        TranslationGlossaryItem("السرعة", "Speed", "Vitesse", "الفيزياء"),
        TranslationGlossaryItem("التسارع / العجلة", "Acceleration", "Accélération", "الفيزياء"),
        TranslationGlossaryItem("قانون نيوتن للحركة", "Newton's Law of Motion", "Loi du mouvement de Newton", "الفيزياء"),
        TranslationGlossaryItem("الطاقة الحركية", "Kinetic Energy", "Énergie cinétique", "الفيزياء"),
        TranslationGlossaryItem("طاقة الوضع", "Potential Energy", "Énergie potentielle", "الفيزياء"),
        TranslationGlossaryItem("المجال المغناطيسي", "Magnetic Field", "Champ magnétique", "الفيزياء"),
        TranslationGlossaryItem("التيار الكهربائي", "Electric Current", "Courant électrique", "الفيزياء"),
        TranslationGlossaryItem("فرق الجهد", "Voltage / Potential Difference", "Différence de potentiel / Tension", "الفيزياء"),
        TranslationGlossaryItem("المقاومة الكهربائية", "Electrical Resistance", "Résistance électrique", "الفيزياء"),
        TranslationGlossaryItem("انكسار الضوء", "Refraction of Light", "Réfraction de la lumière", "الفيزياء"),
        TranslationGlossaryItem("انعكاس الضوء", "Reflection of Light", "Réflexion de la lumière", "الفيزياء"),
        TranslationGlossaryItem("التردد والطول الموجي", "Frequency and Wavelength", "Fréquence et longueur d'onde", "الفيزياء"),
        TranslationGlossaryItem("الضغط الجوي", "Atmospheric Pressure", "Pression atmosphérique", "الفيزياء"),
        TranslationGlossaryItem("الجاذبية الأرضية", "Gravity", "Gravité terrestre", "الفيزياء"),
        TranslationGlossaryItem("الديناميكا الحرارية", "Thermodynamics", "Thermodynamique", "الفيزياء"),
        TranslationGlossaryItem("الكهرومغناطيسية", "Electromagnetism", "Électromagnétisme", "الفيزياء"),
        TranslationGlossaryItem("الفيزياء النووية", "Nuclear Physics", "Physique nucléaire", "الفيزياء"),

        // 3. Chemistry (الكيمياء)
        TranslationGlossaryItem("الجدول الدوري", "Periodic Table", "Tableau périodique", "الكيمياء"),
        TranslationGlossaryItem("الرابطة التساهمية", "Covalent Bond", "Liaison covalente", "الكيمياء"),
        TranslationGlossaryItem("الرابطة الأيونية", "Ionic Bond", "Liaison ionique", "الكيمياء"),
        TranslationGlossaryItem("التفاعل الكيميائي", "Chemical Reaction", "Réaction chimique", "الكيمياء"),
        TranslationGlossaryItem("الأحماض والقواعد", "Acids and Bases", "Acides et bases", "الكيمياء"),
        TranslationGlossaryItem("الرقم الهيدروجيني (pH)", "pH Value", "Valeur du pH", "الكيمياء"),
        TranslationGlossaryItem("الأكسدة والاختزال", "Redox (Oxidation-Reduction)", "Oxydoréduction", "الكيمياء"),
        TranslationGlossaryItem("المحلول المشبع", "Saturated Solution", "Solution saturée", "الكيمياء"),
        TranslationGlossaryItem("المركبات العضوية", "Organic Compounds", "Composés organiques", "الكيمياء"),
        TranslationGlossaryItem("الكتلة المولية", "Molar Mass", "Masse molaire", "الكيمياء"),
        TranslationGlossaryItem("العوامل الحفازة", "Catalysts", "Catalyseurs", "الكيمياء"),
        TranslationGlossaryItem("التوازن الكيميائي", "Chemical Equilibrium", "Équilibre chimique", "الكيمياء"),

        // 4. Biology & Geology (الأحياء والجيولوجيا)
        TranslationGlossaryItem("الخلية النباتية", "Plant Cell", "Cellule végétale", "الأحياء"),
        TranslationGlossaryItem("الخلية الحيوانية", "Animal Cell", "Cellule animale", "الأحياء"),
        TranslationGlossaryItem("الحمض النووي (DNA)", "DNA (Deoxyribonucleic Acid)", "ADN (Acide désoxyribonucléique)", "الأحياء"),
        TranslationGlossaryItem("البناء الضوئي", "Photosynthesis", "Photosynthèse", "الأحياء"),
        TranslationGlossaryItem("الانقسام الميتوزي", "Mitosis", "Mitose", "الأحياء"),
        TranslationGlossaryItem("الانقسام الميوزي", "Meiosis", "Méiose", "الأحياء"),
        TranslationGlossaryItem("الجهاز العصبي", "Nervous System", "Système nerveux", "الأحياء"),
        TranslationGlossaryItem("الدورة الدموية", "Circulatory System", "Système circulatoire", "الأحياء"),
        TranslationGlossaryItem("الجهاز الهضمي", "Digestive System", "Système digestif", "الأحياء"),
        TranslationGlossaryItem("الجهاز التنفسي", "Respiratory System", "Système respiratoire", "الأحياء"),
        TranslationGlossaryItem("علم الوراثة", "Genetics", "Génétique", "الأحياء"),
        TranslationGlossaryItem("طبقات الأرض", "Earth Layers", "Couches terrestres", "الجيولوجيا"),
        TranslationGlossaryItem("الصفائح التكتونية", "Tectonic Plates", "Plaques tectoniques", "الجيولوجيا"),
        TranslationGlossaryItem("الصخور الرسوبية", "Sedimentary Rocks", "Roches sédimentaires", "الجيولوجيا"),
        TranslationGlossaryItem("الصخور النارية", "Igneous Rocks", "Roches magmatiques", "الجيولوجيا"),

        // 5. Classroom & Academic Feedback (توجيهات الحصة والتقييم)
        TranslationGlossaryItem("ممتاز جداً، استمر!", "Excellent, keep it up!", "Excellent, continuez ainsi !", "تقييمات المعلم"),
        TranslationGlossaryItem("أحسنت صنعاً", "Well done", "Bien joué", "تقييمات المعلم"),
        TranslationGlossaryItem("يرجى مراجعة حل السؤال الثاني", "Please review Question 2 solution", "Veuillez revoir la solution de la question 2", "تقييمات المعلم"),
        TranslationGlossaryItem("انتبه للوحدات والتحويلات", "Pay attention to units and conversions", "Faites attention aux unités et conversions", "تقييمات المعلم"),
        TranslationGlossaryItem("واجب منزلي للحصة القادمة", "Homework for next class", "Devoir pour le prochain cours", "تقييمات المعلم"),
        TranslationGlossaryItem("امتحان شامل الأسبوع القادم", "Comprehensive exam next week", "Examen complet la semaine prochaine", "تقييمات المعلم"),
        TranslationGlossaryItem("حضور والتزام رائع", "Great attendance and discipline", "Excellente assiduité et discipline", "تقييمات المعلم"),
        TranslationGlossaryItem("غياب بدون عذر مسبق", "Absence without prior excuse", "Absence sans excuse préalable", "تقييمات المعلم"),
        TranslationGlossaryItem("افتح الكتاب صفحة", "Open the book on page", "Ouvrez le livre à la page", "تعليمات الحصة"),
        TranslationGlossaryItem("اكتب الإجابة في الكشكول", "Write the answer in your notebook", "Écrivez la réponse dans votre cahier", "تعليمات الحصة"),
        TranslationGlossaryItem("ركز على السبورة", "Focus on the whiteboard", "Concentrez-vous sur le tableau", "تعليمات الحصة"),
        TranslationGlossaryItem("انتهى وقت الامتحان", "Exam time is over", "Le temps de l'examen est écoulé", "تعليمات الحصة"),
        TranslationGlossaryItem("الرجاء الهدوء", "Please be quiet", "Silence s'il vous plaît", "تعليمات الحصة"),

        // 6. Languages & Grammar (اللغات والقواعد)
        TranslationGlossaryItem("المبتدأ والخبر", "Subject and Predicate", "Sujet et prédicat", "اللغة العربية"),
        TranslationGlossaryItem("الفعل والفاعل والمفعول به", "Verb, Subject, and Object", "Verbe, sujet et complément d'objet", "اللغة العربية"),
        TranslationGlossaryItem("حروف الجر", "Prepositions", "Prépositions", "اللغة العربية"),
        TranslationGlossaryItem("الممنوع من الصرف", "Dioptotes / Uninflected Nouns", "Noms invariables", "اللغة العربية"),
        TranslationGlossaryItem("الاستعارة والتشبيه", "Metaphor and Simile", "Métaphore et comparaison", "اللغة العربية"),
        TranslationGlossaryItem("المبني للمجهول", "Passive Voice", "Voix passive", "قواعد اللغات"),
        TranslationGlossaryItem("المضارع التام", "Present Perfect", "Passé composé", "قواعد اللغات"),
        TranslationGlossaryItem("الجملة الشرطية", "Conditional Sentence", "Phrase conditionnelle", "قواعد اللغات"),
        TranslationGlossaryItem("الماضي البسيط", "Past Simple", "Passé simple", "قواعد اللغات"),
        TranslationGlossaryItem("المستقبل البسيط", "Future Simple", "Futur simple", "قواعد اللغات"),
        TranslationGlossaryItem("الضمائر الشخصية", "Personal Pronouns", "Pronoms personnels", "قواعد اللغات"),

        // 7. General & Daily Academic Vocabulary (قاموس الكلمات والمصطلحات الشائعة)
        TranslationGlossaryItem("معلم", "Teacher", "Enseignant", "عام"),
        TranslationGlossaryItem("المعلم", "The Teacher", "L'enseignant", "عام"),
        TranslationGlossaryItem("طالب", "Student", "Élève / Étudiant", "عام"),
        TranslationGlossaryItem("الطلاب", "Students", "Les élèves", "عام"),
        TranslationGlossaryItem("مدرسة", "School", "École", "عام"),
        TranslationGlossaryItem("فصل", "Class / Classroom", "Classe", "عام"),
        TranslationGlossaryItem("مجموعة", "Group", "Groupe", "عام"),
        TranslationGlossaryItem("المجموعات", "Groups", "Groupes", "عام"),
        TranslationGlossaryItem("حصة", "Session / Lesson", "Séance / Cours", "عام"),
        TranslationGlossaryItem("جدول", "Schedule", "Emploi du temps", "عام"),
        TranslationGlossaryItem("كتاب", "Book", "Livre", "عام"),
        TranslationGlossaryItem("صفحة", "Page", "Page", "عام"),
        TranslationGlossaryItem("ملف", "File", "Fichier", "عام"),
        TranslationGlossaryItem("صورة", "Image / Picture", "Image / Photo", "عام"),
        TranslationGlossaryItem("صور", "Images / Pictures", "Images / Photos", "عام"),
        TranslationGlossaryItem("جودة", "Quality", "Qualité", "عام"),
        TranslationGlossaryItem("عالية", "High", "Haute", "عام"),
        TranslationGlossaryItem("متوسطة", "Medium", "Moyenne", "عام"),
        TranslationGlossaryItem("منخفضة", "Low", "Basse", "عام"),
        TranslationGlossaryItem("تصدير", "Export", "Exporter", "عام"),
        TranslationGlossaryItem("استخراج", "Extract", "Extraire", "عام"),
        TranslationGlossaryItem("مشاركة", "Share", "Partager", "عام"),
        TranslationGlossaryItem("حفظ", "Save", "Enregistrer", "عام"),
        TranslationGlossaryItem("حذف", "Delete", "Supprimer", "عام"),
        TranslationGlossaryItem("تعديل", "Edit", "Modifier", "عام"),
        TranslationGlossaryItem("إلغاء", "Cancel", "Annuler", "عام"),
        TranslationGlossaryItem("تراجع", "Undo", "Annuler", "عام"),
        TranslationGlossaryItem("إعادة", "Redo", "Rétablir", "عام"),
        TranslationGlossaryItem("بحث", "Search", "Rechercher", "عام"),
        TranslationGlossaryItem("إعدادات", "Settings", "Paramètres", "عام"),
        TranslationGlossaryItem("الإعدادات", "Settings", "Paramètres", "عام"),
        TranslationGlossaryItem("لغة", "Language", "Langue", "عام"),
        TranslationGlossaryItem("اللغة", "Language", "La langue", "عام"),
        TranslationGlossaryItem("العربية", "Arabic", "Arabe", "عام"),
        TranslationGlossaryItem("الإنجليزية", "English", "Anglais", "عام"),
        TranslationGlossaryItem("الفرنسية", "French", "Français", "عام"),
        TranslationGlossaryItem("امتحان", "Exam", "Examen", "عام"),
        TranslationGlossaryItem("اختبار", "Test / Quiz", "Test / Quiz", "عام"),
        TranslationGlossaryItem("درجة", "Grade / Score", "Note", "عام"),
        TranslationGlossaryItem("درجات", "Grades / Scores", "Notes", "عام"),
        TranslationGlossaryItem("نسبة مئوية", "Percentage", "Pourcentage", "عام"),
        TranslationGlossaryItem("واجب", "Homework", "Devoir", "عام"),
        TranslationGlossaryItem("شهادة", "Certificate", "Certificat", "عام"),
        TranslationGlossaryItem("شهادات", "Certificates", "Certificats", "عام"),
        TranslationGlossaryItem("تقرير", "Report", "Rapport", "عام"),
        TranslationGlossaryItem("تقارير", "Reports", "Rapports", "عام"),
        TranslationGlossaryItem("حضور", "Attendance", "Présence", "عام"),
        TranslationGlossaryItem("غياب", "Absence", "Absence", "عام"),
        TranslationGlossaryItem("مالية", "Finance", "Finances", "عام"),
        TranslationGlossaryItem("مدفوعات", "Payments", "Paiements", "عام"),
        TranslationGlossaryItem("رسوم", "Fees", "Frais", "عام"),
        TranslationGlossaryItem("اشتراك", "Subscription", "Abonnement", "عام"),
        TranslationGlossaryItem("سنتر", "Educational Center", "Centre Éducatif", "عام"),
        TranslationGlossaryItem("قاعة", "Hall / Classroom", "Salle", "عام"),
        TranslationGlossaryItem("مذكرة", "Study Booklet / Notes", "Fascicule / Polycopié", "عام"),
        TranslationGlossaryItem("مذكرات", "Booklets / Notes", "Fascicules", "عام"),
        TranslationGlossaryItem("سؤال", "Question", "Question", "عام"),
        TranslationGlossaryItem("أسئلة", "Questions", "Questions", "عام"),
        TranslationGlossaryItem("جواب", "Answer", "Réponse", "عام"),
        TranslationGlossaryItem("إجابة", "Answer / Solution", "Réponse", "عام"),
        TranslationGlossaryItem("إجابات", "Answers", "Réponses", "عام"),
        TranslationGlossaryItem("صحيح", "Correct / True", "Correct / Vrai", "عام"),
        TranslationGlossaryItem("خطأ", "Wrong / False", "Faux / Erreur", "عام"),
        TranslationGlossaryItem("اختيار من متعدد", "Multiple Choice", "Choix multiple (QCM)", "عام"),
        TranslationGlossaryItem("علل", "Give reason / Explain why", "Expliquez pourquoi", "عام"),
        TranslationGlossaryItem("ما المقصود بـ", "What is meant by", "Qu'entend-on par", "عام"),
        TranslationGlossaryItem("قارن بين", "Compare between", "Comparez entre", "عام"),
        TranslationGlossaryItem("أكمل العبارات", "Complete the sentences", "Complétez les phrases", "عام"),
        TranslationGlossaryItem("حل المسألة التالية", "Solve the following problem", "Résolvez le problème suivant", "عام"),
        TranslationGlossaryItem("الصف الأول الابتدائي", "1st Primary Grade", "1ère Année Primaire", "الصفوف"),
        TranslationGlossaryItem("الصف الثاني الابتدائي", "2nd Primary Grade", "2ème Année Primaire", "الصفوف"),
        TranslationGlossaryItem("الصف الثالث الابتدائي", "3rd Primary Grade", "3ème Année Primaire", "الصفوف"),
        TranslationGlossaryItem("الصف الرابع الابتدائي", "4th Primary Grade", "4ème Année Primaire", "الصفوف"),
        TranslationGlossaryItem("الصف الخامس الابتدائي", "5th Primary Grade", "5ème Année Primaire", "الصفوف"),
        TranslationGlossaryItem("الصف السادس الابتدائي", "6th Primary Grade", "6ème Année Primaire", "الصفوف"),
        TranslationGlossaryItem("الصف الأول الإعدادي", "1st Preparatory Grade", "1ère Année Collège", "الصفوف"),
        TranslationGlossaryItem("الصف الثاني الإعدادي", "2nd Preparatory Grade", "2ème Année Collège", "الصفوف"),
        TranslationGlossaryItem("الصف الثالث الإعدادي", "3rd Preparatory Grade", "3ème Année Collège", "الصفوف"),
        TranslationGlossaryItem("الصف الأول الثانوي", "1st Secondary Grade", "1ère Année Lycée", "الصفوف"),
        TranslationGlossaryItem("الصف الثاني الثانوي", "2nd Secondary Grade", "2ème Année Lycée", "الصفوف"),
        TranslationGlossaryItem("الصف الثالث الثانوي", "3rd Secondary Grade", "3ème Année Lycée", "الصفوف"),
        TranslationGlossaryItem("المرحلة الابتدائية", "Primary Stage", "Cycle Primaire", "الصفوف"),
        TranslationGlossaryItem("المرحلة الإعدادية", "Preparatory Stage", "Cycle Collège", "الصفوف"),
        TranslationGlossaryItem("المرحلة الثانوية", "Secondary Stage", "Cycle Lycée", "الصفوف"),
        TranslationGlossaryItem("الابتدائية", "Primary", "Primaire", "الصفوف"),
        TranslationGlossaryItem("الإعدادية", "Preparatory", "Collège", "الصفوف"),
        TranslationGlossaryItem("الثانوية", "Secondary", "Lycée", "الصفوف"),
        TranslationGlossaryItem("أولى", "First / 1st", "Première / 1ère", "الصفوف"),
        TranslationGlossaryItem("تانية", "Second / 2nd", "Deuxième / 2ème", "الصفوف"),
        TranslationGlossaryItem("تالتة", "Third / 3rd", "Troisième / 3ème", "الصفوف"),
        TranslationGlossaryItem("رابعة", "Fourth / 4th", "Quatrième / 4ème", "الصفوف"),
        TranslationGlossaryItem("خامسة", "Fifth / 5th", "Cinquième / 5ème", "الصفوف"),
        TranslationGlossaryItem("سادسة", "Sixth / 6th", "Sixième / 6ème", "الصفوف"),
        TranslationGlossaryItem("نعم", "Yes", "Oui", "عام"),
        TranslationGlossaryItem("لا", "No", "Non", "عام"),
        TranslationGlossaryItem("من فضلك", "Please", "S'il vous plaît", "عام"),
        TranslationGlossaryItem("شكراً", "Thank you", "Merci", "عام"),
        TranslationGlossaryItem("مرحباً", "Welcome / Hello", "Bienvenue / Bonjour", "عام"),
        TranslationGlossaryItem("صباح الخير", "Good morning", "Bonjour", "عام"),
        TranslationGlossaryItem("مساء الخير", "Good evening", "Bonsoir", "عام"),
        TranslationGlossaryItem("مع السلامة", "Goodbye", "Au revoir", "عام"),
        TranslationGlossaryItem("اليوم", "Today", "Aujourd'hui", "عام"),
        TranslationGlossaryItem("غداً", "Tomorrow", "Demain", "عام"),
        TranslationGlossaryItem("أمس", "Yesterday", "Hier", "عام"),
        TranslationGlossaryItem("الوقت", "Time", "Le temps / L'heure", "عام"),
        TranslationGlossaryItem("التاريخ", "Date", "La date", "عام"),
        TranslationGlossaryItem("العنوان", "Address / Title", "Adresse / Titre", "عام"),
        TranslationGlossaryItem("الهاتف", "Phone", "Téléphone", "عام"),
        TranslationGlossaryItem("واتساب", "WhatsApp", "WhatsApp", "عام"),
        TranslationGlossaryItem("ولي الأمر", "Parent / Guardian", "Parent / Tuteur", "عام"),
        TranslationGlossaryItem("أولياء الأمور", "Parents", "Les parents", "عام")
    )

    /**
     * Normalizes Arabic strings for robust search and matching.
     */
    private fun normalizeArabic(input: String): String {
        return input.trim()
            .replace(Regex("[\u064B-\u065F\u0670]"), "") // remove tashkeel
            .replace("أ", "ا")
            .replace("إ", "ا")
            .replace("آ", "ا")
            .replace("ة", "ه")
            .replace("ى", "ي")
    }

    /**
     * Normalizes Latin (EN/FR) text.
     */
    private fun normalizeLatin(input: String): String {
        return input.trim().lowercase()
            .replace("’", "'")
            .replace(Regex("[.,;:!?()\"'\\[\\]]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Intelligent multi-tier translation between Arabic, English, and French.
     */
    fun translate(
        text: String,
        fromLanguage: AppLanguage,
        toLanguage: AppLanguage
    ): String {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return ""
        if (fromLanguage == toLanguage) return trimmed

        // Check if text has multiple lines
        val lines = text.split("\n")
        if (lines.size > 1) {
            return lines.joinToString("\n") { line ->
                translateSingleLine(line, fromLanguage, toLanguage)
            }
        }

        return translateSingleLine(trimmed, fromLanguage, toLanguage)
    }

    private fun translateSingleLine(
        text: String,
        fromLanguage: AppLanguage,
        toLanguage: AppLanguage
    ): String {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return ""

        // 1. Direct Full-phrase Match
        val directMatch = findGlossaryMatch(trimmed, fromLanguage)
        if (directMatch != null) {
            return getTargetString(directMatch, toLanguage)
        }

        // 2. Sliding Window N-gram Token Matching (greedy longest match)
        val tokens = tokenizeWithPunctuation(trimmed)
        if (tokens.isEmpty()) return trimmed

        val resultTokens = mutableListOf<String>()
        var i = 0

        while (i < tokens.size) {
            val token = tokens[i]

            // If it's punctuation or whitespace or pure number, keep as-is
            if (token.matches(Regex("^[\\p{Punct}\\s\\d]+$"))) {
                resultTokens.add(token)
                i++
                continue
            }

            var matched = false
            // Try window sizes from 5 down to 1
            for (windowSize in 5 downTo 1) {
                if (i + windowSize <= tokens.size) {
                    val phrase = tokens.subList(i, i + windowSize)
                        .filter { !it.matches(Regex("^\\s+$")) }
                        .joinToString(" ")

                    val match = findGlossaryMatch(phrase, fromLanguage)
                    if (match != null) {
                        resultTokens.add(getTargetString(match, toLanguage))
                        i += windowSize
                        matched = true
                        break
                    }
                }
            }

            if (!matched) {
                // Try Single Word with Stemming / Morphology
                val translatedSingle = translateWordMorphologically(token, fromLanguage, toLanguage)
                resultTokens.add(translatedSingle)
                i++
            }
        }

        val assembled = resultTokens.joinToString(" ")
            .replace(" ,", ",")
            .replace(" .", ".")
            .replace(" !", "!")
            .replace(" ?", "?")
            .replace(" :", ":")
            .replace(Regex("\\s+"), " ")
            .trim()

        return assembled.ifEmpty { trimmed }
    }

    private fun tokenizeWithPunctuation(text: String): List<String> {
        val pattern = Regex("([\\p{Punct}]+|\\s+|[^\\p{Punct}\\s]+)")
        return pattern.findAll(text).map { it.value }.toList()
    }

    private fun findGlossaryMatch(query: String, fromLanguage: AppLanguage): TranslationGlossaryItem? {
        val clean = query.trim()
        if (clean.isEmpty()) return null

        return when (fromLanguage) {
            AppLanguage.ARABIC -> {
                val normQuery = normalizeArabic(clean)
                educationalGlossary.firstOrNull { normalizeArabic(it.arabic) == normQuery }
                    ?: educationalGlossary.firstOrNull { normalizeArabic(it.arabic).contains(normQuery) || normQuery.contains(normalizeArabic(it.arabic)) }
            }
            AppLanguage.ENGLISH -> {
                val normQuery = normalizeLatin(clean)
                educationalGlossary.firstOrNull { normalizeLatin(it.english) == normQuery }
                    ?: educationalGlossary.firstOrNull { normalizeLatin(it.english).contains(normQuery) || normQuery.contains(normalizeLatin(it.english)) }
            }
            AppLanguage.FRENCH -> {
                val normQuery = normalizeLatin(clean)
                educationalGlossary.firstOrNull { normalizeLatin(it.french) == normQuery }
                    ?: educationalGlossary.firstOrNull { normalizeLatin(it.french).contains(normQuery) || normQuery.contains(normalizeLatin(it.french)) }
            }
        }
    }

    private fun getTargetString(item: TranslationGlossaryItem, toLanguage: AppLanguage): String {
        return when (toLanguage) {
            AppLanguage.ARABIC -> item.arabic
            AppLanguage.ENGLISH -> item.english
            AppLanguage.FRENCH -> item.french
        }
    }

    private fun translateWordMorphologically(word: String, fromLanguage: AppLanguage, toLanguage: AppLanguage): String {
        val trimmed = word.trim()
        if (trimmed.isEmpty()) return ""

        val direct = findGlossaryMatch(trimmed, fromLanguage)
        if (direct != null) return getTargetString(direct, toLanguage)

        // Arabic Morphological Prefix & Suffix Stripping
        if (fromLanguage == AppLanguage.ARABIC) {
            val prefixes = listOf("ال", "لل", "و", "ف", "ب", "ك", "ل")
            val suffixes = listOf("ات", "ين", "ون", "ية", "ها", "هم", "هن", "نا", "كم", "ك", "ي", "ه")

            for (prefix in prefixes) {
                if (trimmed.startsWith(prefix) && trimmed.length > prefix.length + 2) {
                    val root = trimmed.substring(prefix.length)
                    val match = findGlossaryMatch(root, fromLanguage)
                    if (match != null) {
                        val trans = getTargetString(match, toLanguage)
                        return when (toLanguage) {
                            AppLanguage.ENGLISH -> if (prefix == "ال" || prefix == "لل") "the $trans" else if (prefix == "و") "and $trans" else trans
                            AppLanguage.FRENCH -> if (prefix == "ال" || prefix == "لل") "le $trans" else if (prefix == "و") "et $trans" else trans
                            AppLanguage.ARABIC -> trans
                        }
                    }
                }
            }

            for (suffix in suffixes) {
                if (trimmed.endsWith(suffix) && trimmed.length > suffix.length + 2) {
                    val root = trimmed.substring(0, trimmed.length - suffix.length)
                    val match = findGlossaryMatch(root, fromLanguage)
                    if (match != null) {
                        val trans = getTargetString(match, toLanguage)
                        return trans
                    }
                }
            }
        }

        // English Morphology (plurals, -ing, -ed, -ly)
        if (fromLanguage == AppLanguage.ENGLISH) {
            val lower = trimmed.lowercase()
            if (lower.endsWith("s") && lower.length > 3) {
                val match = findGlossaryMatch(lower.substring(0, lower.length - 1), fromLanguage)
                if (match != null) return getTargetString(match, toLanguage)
            }
            if (lower.endsWith("ing") && lower.length > 4) {
                val match = findGlossaryMatch(lower.substring(0, lower.length - 3), fromLanguage)
                if (match != null) return getTargetString(match, toLanguage)
            }
            if (lower.endsWith("ed") && lower.length > 3) {
                val match = findGlossaryMatch(lower.substring(0, lower.length - 2), fromLanguage)
                if (match != null) return getTargetString(match, toLanguage)
            }
        }

        // French Morphology (plurals -s, -es)
        if (fromLanguage == AppLanguage.FRENCH) {
            val lower = trimmed.lowercase()
            if (lower.endsWith("s") && lower.length > 3) {
                val match = findGlossaryMatch(lower.substring(0, lower.length - 1), fromLanguage)
                if (match != null) return getTargetString(match, toLanguage)
            }
        }

        // Fallback: Return original token
        return trimmed
    }
}
