package blender_clj;

import com.sun.jna.Pointer;

public class DirectMapped
{
  public static native Pointer PyCapsule_GetPointer(Pointer capsule, String name);
}
