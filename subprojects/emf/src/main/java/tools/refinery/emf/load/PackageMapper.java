package tools.refinery.emf.load;

import org.eclipse.emf.ecore.*;
import tools.refinery.emf.utility.PackageMap;

import java.util.HashMap;
import java.util.Map;

public class PackageMapper {
	private PackageMap map = new PackageMap(){};
	private final Map<EModelElement, String> classifierTrace = new HashMap<>();
	private final String newLine = System.getProperty("line.separator");
	public String map(EPackage ePackage){
		var builder = new StringBuilder();
		for(EClassifier eClassifier : ePackage.getEClassifiers()){
			builder.append(map(eClassifier));
		}
		return builder.toString();
	}

	public String map(EClassifier eClassifier){
		return switch (eClassifier){
			case EClass eClass -> map(eClass);
			case EEnum eEnum -> map(eEnum);
			case EDataType eDataType -> "% EDataType not supported. %s".formatted(map.name(eDataType));
			default -> throw new IllegalArgumentException("Unknown EClassifier: "+eClassifier.getName());
		};
	}
	public String map(EClass eClass){
		var builder = new StringBuilder();
		if(eClass.isAbstract()){
			builder.append("abstract ");
		}
		builder.append("class ");

		var name = map.name(eClass);
		builder.append(name);
		classifierTrace.put(eClass, name);
		var it = eClass.getESuperTypes().iterator();
		while (it.hasNext()){
			var superType = it.next();
			builder.append(map.name(superType));
			if(it.hasNext()){
				builder.append(", ");
			} else {
				builder.append(" ");
			}
		}
		builder.append("{");
		builder.append(newLine);

		for(EStructuralFeature eStructuralFeature : eClass.getEStructuralFeatures()){
			builder.append("\t");
			builder.append(map(eStructuralFeature));
			builder.append(newLine);
		}
		builder.append("}");
		return builder.toString();
	}
	public String map(EEnum eEnum){
		var builder = new StringBuilder();
		builder.append("enum ");

		var name = map.name(eEnum);
		builder.append(name);
		classifierTrace.put(eEnum, name);
		builder.append(" {");
		builder.append(newLine);

		var it = eEnum.getELiterals().iterator();
		while (it.hasNext()){
			var literal = it.next();
			var lname = map.name(literal);
			classifierTrace.put(eEnum, lname);
			builder.append(lname);
			if(it.hasNext()){
				builder.append(",");
			}
			builder.append(newLine);
		}
		builder.append("}");
		return builder.toString();
	}
	public String map(EStructuralFeature eStructuralFeature){
		return switch (eStructuralFeature){
			case EReference eReference -> map(eReference);
			case EAttribute eAttribute -> map(eAttribute);
			default -> throw new IllegalArgumentException("Unknown EStructuralFeature: "+eStructuralFeature.getName());
		};
	}

	public String map(EReference eReference){
		var builder = new StringBuilder();
		if(eReference.isContainer()){
			builder.append("container ");
		}
		if(eReference.isContainment()){
			builder.append("contains ");
		}
		builder.append(map.name(eReference.getEReferenceType()));
		if (eReference.isMany()){
			builder.append("[] ");
		} else {
			builder.append(" ");
		}

		var name = map.name(eReference);
		classifierTrace.put(eReference, name);
		builder.append(name);

		if(eReference.getEOpposite()!=null){
			var opposite = eReference.getEOpposite();
			builder.append(" opposite ");
			builder.append(map.name(opposite));
		}
		return builder.toString();
	}
	public String map(EAttribute eAttribute){
		//TODO check EEnum
		return "% Attribute not supported. %s".formatted(map.name(eAttribute));
	}
}
