package tools.refinery.emf.view;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.resource.Resource;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class EClassView implements EObject {
	private final EClass eClass;
	private final Map<String, EObject> trace = new HashMap<>();
	private EClassView(EClass eClass){
		this.eClass = eClass;
	}

	@Override
	public EClass eClass() {
		return eClass;
	}

	@Override
	public Resource eResource() {
		return null;
	}

	@Override
	public EObject eContainer() {
		return null;
	}

	@Override
	public EStructuralFeature eContainingFeature() {
		return eClass.eContainingFeature();
	}

	@Override
	public EReference eContainmentFeature() {
		return eClass.eContainmentFeature();
	}

	@Override
	public EList<EObject> eContents() {
		return null;
	}

	@Override
	public TreeIterator<EObject> eAllContents() {
		return null;
	}

	@Override
	public boolean eIsProxy() {
		return eClass.eIsProxy();
	}

	@Override
	public EList<EObject> eCrossReferences() {
		return null;
	}

	@Override
	public Object eGet(EStructuralFeature eStructuralFeature) {
		return null;
	}

	@Override
	public Object eGet(EStructuralFeature eStructuralFeature, boolean b) {
		return null;
	}

	@Override
	public void eSet(EStructuralFeature eStructuralFeature, Object o) {

	}

	@Override
	public boolean eIsSet(EStructuralFeature eStructuralFeature) {
		return false;
	}

	@Override
	public void eUnset(EStructuralFeature eStructuralFeature) {

	}

	@Override
	public Object eInvoke(EOperation eOperation, EList<?> eList) throws InvocationTargetException {
		return null;
	}

	@Override
	public EList<Adapter> eAdapters() {
		return null;
	}

	@Override
	public boolean eDeliver() {
		return false;
	}

	@Override
	public void eSetDeliver(boolean b) {

	}

	@Override
	public void eNotify(Notification notification) {

	}
}
