package org.opennms.features.topology.app.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.opennms.features.topology.api.BoundingBox;
import org.opennms.features.topology.api.GraphContainer;
import org.opennms.features.topology.api.Layout;
import org.opennms.features.topology.api.Point;
import org.opennms.features.topology.api.topo.Vertex;
import org.opennms.features.topology.api.topo.VertexRef;

public class DefaultLayout implements Layout {
	
	private static final Point ORIGIN = new Point(0,0);

	private GraphContainer m_graphContainer;
	
	private final Map<VertexRef, Point> m_locations = new HashMap<VertexRef, Point>();

	public DefaultLayout(GraphContainer graphContainer) {
		m_graphContainer = graphContainer;
	}

	@Override
	public Point getLocation(VertexRef v) {
		Point p = m_locations.get(v);
		Point location = p == null ? random(1000, 1000) : p;
		return location;
	}
	
	public void setLocation(VertexRef v, int x, int y) {
		setLocation(v, new Point(x, y));
	}

	@Override
	public void setLocation(VertexRef v, Point location) {
		m_locations.put(v, location);
	}

	@Override
	public Point getInitialLocation(VertexRef v) {
		Vertex parent = m_graphContainer.getParent(v);
		return parent == null ? random(1000, 1000) : getLocation(parent);
	}
	
	private int random(int max) {
		return (int)(Math.random()*max);
	}
	
	private Point random(int maxX, int maxY) {
		return new Point(random(maxX), random(maxY));
	}

    @Override
    public BoundingBox getBounds() {
        Collection<? extends Vertex> vertices = m_graphContainer.getGraph().getDisplayVertices();
        if(vertices.size() > 0) {
            Collection<VertexRef> vRefs = new ArrayList<VertexRef>();
            for(Vertex v : vertices) {
                vRefs.add(v);
            }
        
            return computeBoundingBox(vRefs);
        } else {
            BoundingBox bBox = new BoundingBox();
            bBox.addPoint(new Point(0,0));
            return bBox;
        }
    }
    
    private BoundingBox computeBoundingBox(VertexRef vertRef) {
        return new BoundingBox(getLocation(vertRef), 100, 100);
    }
    
    public BoundingBox computeBoundingBox(Collection<VertexRef> vertRefs) {
        if(vertRefs.size() > 0) {
            BoundingBox boundingBox = new BoundingBox();
            for(VertexRef vertRef : vertRefs) {
                boundingBox.addBoundingbox( computeBoundingBox(vertRef) );
            }
            return boundingBox;
        }else {
            return getBounds();
        }
        
    }

}
