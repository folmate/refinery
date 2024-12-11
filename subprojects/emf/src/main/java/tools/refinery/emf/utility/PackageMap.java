package tools.refinery.emf.utility;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EReference;

public interface PackageMap {
	default String name(EClassifier eClassifier){return eClassifier.getName();}
	default String name(EReference eReference){return eReference.getName();}
	default String name(EAttribute eAttribute){return eAttribute.getName();}
	default String name(EEnumLiteral eEnumLiteral){return eEnumLiteral.getName();}
}
