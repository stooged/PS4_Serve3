package stooged.ps4serve3;

public class Sorter {
    public void sort(sFile[] files)
    {
        int i, j;
        sFile temp;
        for ( i = 0;  i < files.length - 1;  i++ )
        {
            for ( j = i + 1;  j < files.length;  j++ )
            {
                if ( files [ i ].label.compareToIgnoreCase( files [ j ].label ) > 0 )
                {
                    temp = files [ i ];
                    files [ i ] = files [ j ];
                    files [ j ] = temp;
                }
            }
        }
    }


}
